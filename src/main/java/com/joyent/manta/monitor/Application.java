/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.joyent.manta.client.MantaClient;
import com.joyent.manta.exception.MantaClientHttpResponseException;
import com.joyent.manta.exception.MantaErrorCode;
import com.joyent.manta.monitor.chains.ChainRunner;
import com.joyent.manta.monitor.chains.MantaOperationsChain;
import com.joyent.manta.monitor.config.Configuration;
import com.joyent.manta.monitor.config.Runner;
import io.honeybadger.reporter.HoneybadgerUncaughtExceptionHandler;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Histogram;
import io.prometheus.client.hotspot.DefaultExports;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the main entry point into the Manta Monitor application.
 */
public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    private static final int WAIT_TIME_FOR_JMX_STATS_MS = 3_000;
    private static final HoneybadgerUncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER;
    static {
        UNCAUGHT_EXCEPTION_HANDLER = HoneybadgerUncaughtExceptionHandler.registerAsUncaughtExceptionHandler();
    }

    /**
     * Entry point to the application.
     * @param args requires a single element array with the first element being the URI to a config file
     * @throws InterruptedException thrown when interrupted
     */
    public static void main(final String[] args) throws InterruptedException {
        if (args.length == 0) {
            System.err.println("Manta monitor requires a single parameter "
                    + "specifying the URL its the JSON configuration file");
            System.exit(1);
        }

        int jettyServerPort = validateJettyServerPort(System.getenv("JETTY_SERVER_PORT"));
        final URI configUri = Objects.requireNonNull(parseConfigFileURI(args[0]));
        final MantaMonitorModule module = new MantaMonitorModule(UNCAUGHT_EXCEPTION_HANDLER,
                UNCAUGHT_EXCEPTION_HANDLER.getReporter(), configUri);
        final MantaMonitorServletModule mantaMonitorServletModule = new MantaMonitorServletModule();
        final JettyServerBuilderModule jettyServerBuilderModule = new JettyServerBuilderModule(jettyServerPort);
        final Injector injector = Guice.createInjector(module, mantaMonitorServletModule, jettyServerBuilderModule);
        final Configuration configuration = injector.getInstance(Configuration.class);
        LOG.info("Starting Manta Monitor");
        final MantaMonitorJerseyServer server = injector.getInstance(MantaMonitorJerseyServer.class);

        // DefaultExports registers collectors, built into the prometheus java
        // client, for garbage collection, memory pools, JMX, classloading, and
        // thread counts
        DefaultExports.initialize();

        try {
            server.start();
        } catch (Exception e) {
            copyContextToException(injector, jettyServerPort, e, "Failed to start Embedded Jetty Server at port");
            System.exit(1);
        }

        try (MantaClient client = injector.getInstance(MantaClient.class)) {
            final Set<ChainRunner> runningChains = startAllChains(configuration, injector, client);

            while (!runningChains.isEmpty()) {
                runningChains.removeIf(chainRunner -> !chainRunner.isRunning());
                Thread.sleep(2000);
            }
        }

        LOG.info("Stopping Manta Monitor Web");
        try {
            server.stop();
        } catch (Exception e) {
            copyContextToException(injector, jettyServerPort, e, "Failed to stop Embedded Jetty Server at port");
            System.exit(1);
        }
    }

    /**
     * Iterates through all of the monitor test chains defined in the
     * configuration and starts them.
     *
     * @param configuration configuration to load
     * @param injector Guice DI object
     * @param client MantaClient object
     * @return a set of all chain runners started
     */
    private static Set<ChainRunner> startAllChains(final Configuration configuration,
                                                   final Injector injector,
                                                   final MantaClient client) {
        final Set<ChainRunner> runningChains =
                new LinkedHashSet<>(configuration.getTestRunners().size());
        final String testType = configuration.getTestType();
        /*
         * A shared map for storing the Histogram object for each chain.
         * This map is shared across all the running chains with key as the name
         * of the chain class and value as the histogram object for that chain.
         * Each chain will thereby be using the same histogram object to record
         * the time elapsed during the execution of put directory and put file request.
         */
        final Map<String, Histogram> requestPutHistogramsMap =
                injector.getInstance(
                        Key.get(new TypeLiteral<ConcurrentHashMap<String, Histogram>>() { }
                        ));
        /* We programmatically load each monitor test chain as specified by the
         * configuration file. */

        // Create all of the chains and prepare them for their run
        for (Runner runner : configuration.getTestRunners()) {
            try {
                @SuppressWarnings("unchecked")
                Class<MantaOperationsChain> chainClass =
                        (Class<MantaOperationsChain>)Class.forName(runner.getChainClassName());
                MantaOperationsChain chain = injector.getInstance(chainClass);
                String metricPostFix = chain.getClass().getSimpleName();
                String putRequestLatencyMetric = String.format("manta_monitor_%s_put_request_latency_", testType);

                if ("buckets".equals(testType)) {
                    client.options(client.getContext().getMantaBucketsDirectory());
                    if ("FileMultipartUploadGetDeleteChain".equals(metricPostFix)) {
                        String msg = "Multipart upload not supported for buckets yet."
                                + "Check the CONFIG_FILE env variable and re-run the application";
                        LOG.error(msg);
                        System.exit(1);
                    }
                }

                Histogram requestPutHistogram = Histogram.build()
                        .name(putRequestLatencyMetric + metricPostFix)
                        .help("Metric that gives a cumulative observation for "
                                + "latency, in seconds, in creating a directory/bucket and "
                                + "putting an object")
                        .create();
                // Add the histogram here, to register it later below
                requestPutHistogramsMap.put(chain.getClass().getSimpleName(),
                        requestPutHistogram);
                ChainRunner chainRunner = new ChainRunner(chain, runner,
                        client, UNCAUGHT_EXCEPTION_HANDLER, requestPutHistogramsMap, testType);
                runningChains.add(chainRunner);
            } catch (ClassNotFoundException e) {
                LOG.error("Unable to load class: {}", runner.getChainClassName());
            } catch (IOException e) {
                // Indicates that manta end point is not compatible to support bucket
                // operations and hence we cannot continue further.
                if (e instanceof MantaClientHttpResponseException) {
                    if (((MantaClientHttpResponseException)e)
                            .getServerCode()
                            .equals(MantaErrorCode.RESOURCE_NOT_FOUND_ERROR)) {
                        LOG.error("Buckets not supported in current manta");
                        System.exit(1);
                    }
                }
            }
        }

        // Provide the testType parameter to create the right CustomPrometheusCollector.
        final CustomPrometheusCollector collector;
        final CustomPrometheusCollectorFactory collectorFactory =
                injector.getInstance(CustomPrometheusCollectorFactory.class);
        if ("buckets".equals(testType)) {
            collector = collectorFactory.create("buckets");
        } else {
            collector = collectorFactory.create("dir");
        }

        // Start each chain
        for (ChainRunner runner : runningChains) {
            runner.start();
        }

        for (boolean jmxStatsAvailable = false; !jmxStatsAvailable;) {
            try {
                CollectorRegistry.defaultRegistry.register(collector);
                /* Use the shared map from above to register the histograms,
                 * for each running chain, to the defaultRegistry. */
                requestPutHistogramsMap.forEach((key, value) -> {
                    value.register();
                });
                jmxStatsAvailable = true;
            } catch (MBeanServerOperationException e) {
                // Wait for the JMX stats to be available for an addition 2 seconds
                try {
                    LOG.debug("JMX stats not available yet, waiting another {} ms",
                            WAIT_TIME_FOR_JMX_STATS_MS);
                    Thread.sleep(WAIT_TIME_FOR_JMX_STATS_MS);
                } catch (InterruptedException ie) {
                    // Pass along the interruption to the main thread
                    Thread.currentThread().interrupt();
                }
            }
        }

        return runningChains;
    }

    private static URI parseConfigFileURI(final String uriString) {
        URI configUri = null;

        try {
            configUri = URI.create(uriString);

            if (configUri.getScheme() == null) {
                File file = new File(uriString);

                if (!file.exists()) {
                    LOG.error("File does not exist: {}", uriString);
                    System.exit(1);
                }

                try {
                    configUri = file.getCanonicalFile().toURI();
                } catch (IOException ioe) {
                    LOG.error("Unable to convert file to URI", ioe);
                    System.exit(1);
                }
            }

        } catch (IllegalArgumentException | NullPointerException e) {
            String msg = String.format("Invalid URI for configuration file: %s",
                    uriString);
            LOG.error(msg, e);
            System.exit(1);
        }

        return configUri;
    }

    private static void copyContextToException(final Injector injector,
                                               final int jettyServerPort,
                                               final Exception e,
                                               final String s) {
        InstanceMetadata instanceMetadata = injector.getInstance(InstanceMetadata.class);
        StringBuilder messageBuffer = new StringBuilder();
        instanceMetadata.asMap().forEach((key, value) -> {
            messageBuffer.append(System.lineSeparator()).append(key).append(" : ").append(value);
        });

        String message = String.format("%s %d. Additional context is as follows:%s",
                s, jettyServerPort, messageBuffer);

        LOG.error(message, e);
    }

    private static int validateJettyServerPort(final String jettyServerPortEnvVariable) {
        if (BooleanUtils.toBoolean(System.getenv("ENABLE_TLS"))) {
            // JETTY_SERVER_PORT is redundant in this case.
            return 0;
        }
        if (jettyServerPortEnvVariable == null) {
            throw new IllegalArgumentException("Missing env variable JETTY_SERVER_PORT");
        }
        if (Integer.parseInt(jettyServerPortEnvVariable) <= 0) {
            throw new IllegalArgumentException("Jetty server port must be greater than 0");
        } else {
            return Integer.parseInt(jettyServerPortEnvVariable);
        }
    }
}
