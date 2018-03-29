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
import io.honeybadger.reporter.HoneybadgerUncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    private static final HoneybadgerUncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER;

    static {
        UNCAUGHT_EXCEPTION_HANDLER = HoneybadgerUncaughtExceptionHandler.registerAsUncaughtExceptionHandler();
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length == 0) {
            System.err.println("Manta monitor requires a single parameter "
                    + "specifying the URL its the JSON configuration file");
            System.exit(1);
        }

        final URI configUri = Objects.requireNonNull(parseConfigFileURI(args[0]));
        final MantaMonitorModule module = new MantaMonitorModule(UNCAUGHT_EXCEPTION_HANDLER,
                UNCAUGHT_EXCEPTION_HANDLER.getReporter(), configUri);
        final Injector injector = Guice.createInjector(module);
        final MantaClient client = injector.getInstance(MantaClient.class);
        final Configuration configuration = injector.getInstance(Configuration.class);

        final Set<ChainRunner> runningChains = new LinkedHashSet<>(configuration.getTestRunners().size());

        for (Runner runner : configuration.getTestRunners()) {
            try {
                @SuppressWarnings("unchecked")
                final MantaOperationsChain chain = (MantaOperationsChain)
                        injector.getInstance(Class.forName(runner.getChainClassName()));
                ChainRunner chainRunner = new ChainRunner(chain, runner.getName(),
                        runner.getThreads(), client, UNCAUGHT_EXCEPTION_HANDLER);
                runningChains.add(chainRunner);
                chainRunner.start();
            } catch (ClassNotFoundException e) {
                LOG.error("Unable to load class: {}", runner.getChainClassName());
            }
        }

        while (!runningChains.isEmpty()) {
            runningChains.removeIf(chainRunner -> !chainRunner.isRunning());
            Thread.currentThread().wait(2000);
        }
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
}
