/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.joyent.manta.client.MantaClient;
import com.joyent.manta.monitor.config.Configuration;
import com.joyent.manta.monitor.config.ConfigurationProvider;
import io.honeybadger.reporter.NoticeReporter;

import java.net.URI;

/**
 * This implementation of {@link Module} provides dependency injection for the application.
 */
public class MantaMonitorModule implements Module {
    private final Thread.UncaughtExceptionHandler honeyBadgerHandler;
    private final NoticeReporter noticeReporter;
    private final io.honeybadger.reporter.config.ConfigContext hbConfig;
    private final URI configURI;

    public MantaMonitorModule(final Thread.UncaughtExceptionHandler uncaughtExceptionHandler,
                              final NoticeReporter noticeReporter,
                              final URI configUri) {
        this.honeyBadgerHandler = uncaughtExceptionHandler;
        this.noticeReporter = noticeReporter;
        this.hbConfig = noticeReporter.getConfig();
        this.configURI = configUri;

        validateHoneybadgerConfig();
    }

    /**
     * Validates the Honeybadger configuration and exits the application if it
     * is not properly set up.
     */
    private void validateHoneybadgerConfig() {
        if (hbConfig.getEnvironment() == null) {
            final String msg = "Manta monitor requires the environment to be "
                    + "set for Honeybadger. See: "
                    + "https://github.com/honeybadger-io/honeybadger-java#configuration-options";
            System.err.println(msg);
            System.exit(1);
        }
        if (hbConfig.getApiKey() == null) {
            final String msg = "Manta monitor requires the Honeybadger API key to be "
                    + "set. See: "
                    + "https://github.com/honeybadger-io/honeybadger-java#configuration-options";
            System.err.println(msg);
            System.exit(1);
        }
    }

    @Override
    public void configure(final Binder binder) {
        binder.bind(PlatformMbeanServerProvider.class).asEagerSingleton();
        binder.bind(JMXMetricsCollector.class).annotatedWith(Names.named("JMXMetricsCollector")).to(JMXMetricsCollector.class).asEagerSingleton();
        binder.bind(CustomPrometheusCollector.class).asEagerSingleton();
        binder.bind(InstanceMetadata.class).asEagerSingleton();
        binder.bind(io.honeybadger.reporter.config.ConfigContext.class).toInstance(hbConfig);
        binder.bind(NoticeReporter.class).toInstance(noticeReporter);
        binder.bind(Thread.UncaughtExceptionHandler.class).toInstance(honeyBadgerHandler);
        binder.bind(com.joyent.manta.config.ConfigContext.class).toProvider(MantaConfigContextProvider.class);
        binder.bind(MantaClient.class).toProvider(MantaClientProvider.class).asEagerSingleton();
        binder.bind(URI.class).annotatedWith(Names.named("configUri")).toInstance(configURI);
        binder.bind(Configuration.class).toProvider(ConfigurationProvider.class);
    }
}
