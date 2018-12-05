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
import com.joyent.manta.client.MantaClient;
import com.joyent.manta.monitor.chains.ChainRunner;
import com.joyent.manta.monitor.chains.MantaOperationsChain;
import com.joyent.manta.monitor.config.Configuration;
import com.joyent.manta.monitor.config.Runner;
import io.logz.guice.jersey.JerseyServer;
import io.honeybadger.reporter.HoneybadgerUncaughtExceptionHandler;
import io.prometheus.client.hotspot.DefaultExports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This is the main entry point into the Manta Monitor application.
 */
public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
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

        final URI configUri = Objects.requireNonNull(parseConfigFileURI(args[0]));
        final MantaMonitorModule module = new MantaMonitorModule(UNCAUGHT_EXCEPTION_HANDLER,
                UNCAUGHT_EXCEPTION_HANDLER.getReporter(), configUri);
        final MantaMonitorServletModule mantaMonitorServletModule = new MantaMonitorServletModule();
        final Injector injector = Guice.createInjector(module);
        final Configuration configuration = injector.getInstance(Configuration.class);
        final Injector jettyServerBuilderInjector = injector.createChildInjector(new JettyServerBuilderModule(injector), mantaMonitorServletModule);

        LOG.info("Starting Manta Monitor");
        final JerseyServer server = jettyServerBuilderInjector.getInstance(JerseyServer.class);
        DefaultExports.initialize();
        try {
            server.start();
        } catch (Exception e) {
            copyContextToException(injector, configuration, e, "Failed to start Embedded Jetty Server at port");
            System.exit(1);
        }

        final Set<ChainRunner> runningChains = startAllChains(configuration, injector);

        while (!runningChains.isEmpty()) {
            runningChains.removeIf(chainRunner -> !chainRunner.isRunning());
            Thread.sleep(2000);
        }

        LOG.info("Stopping Manta Monitor Web");
        try {
            server.stop();
        } catch (Exception e) {
            copyContextToException(injector, configuration, e, "Failed to stop Embedded Jetty Server at port");
            System.exit(1);
        }
    }

    /**
     * Iterates through all of the monitor test chains defined in the
     * configuration and starts them.
     *
     * @param configuration configuration to load
     * @param injector Guice DI object
     * @return a set of all chain runners started
     */
    private static Set<ChainRunner> startAllChains(final Configuration configuration,
                                                   final Injector injector) {
        final Set<ChainRunner> runningChains = new LinkedHashSet<>(configuration.getTestRunners().size());
        final MantaClient client = injector.getInstance(MantaClient.class);

        /* We programmatically load each monitor test chain as specified by the
         * configuration file. */
        for (Runner runner : configuration.getTestRunners()) {
            try {
                @SuppressWarnings("unchecked")
                Class<MantaOperationsChain> chainClass =
                        (Class<MantaOperationsChain>)Class.forName(runner.getChainClassName());
                MantaOperationsChain chain = injector.getInstance(chainClass);
                ChainRunner chainRunner = new ChainRunner(chain, runner,
                        client, UNCAUGHT_EXCEPTION_HANDLER);
                runningChains.add(chainRunner);
                chainRunner.start();
            } catch (ClassNotFoundException e) {
                LOG.error("Unable to load class: {}", runner.getChainClassName());
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
                                               final Configuration configuration,
                                               final Exception e,
                                               final String s) {
        InstanceMetadata instanceMetadata = injector.getInstance(InstanceMetadata.class);
        StringBuilder messageBuffer = new StringBuilder();
        instanceMetadata.asMap().forEach((key, value) -> {
            messageBuffer.append(System.lineSeparator()).append(key).append(" : ").append(value);
        });

        String message = String.format("%s %d. Additional context is as follows:%s",
                s, configuration.getJettyServerPort(), messageBuffer);

        LOG.error(message, e);
    }
}
