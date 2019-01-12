/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.joyent.manta.config.ConfigContext;
import com.joyent.manta.config.MapConfigContext;
import com.joyent.manta.config.MetricReporterMode;
import com.joyent.manta.config.SystemSettingsConfigContext;
import com.joyent.manta.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import java.util.Properties;

/**
 * Provides a {@link com.joyent.manta.config.ConfigContext} with default
 * values relevant to the Manta Monitor (this application).
 */
public class MantaConfigContextProvider implements Provider<ConfigContext> {
    private static final Logger LOG = LoggerFactory.getLogger(MantaConfigContextProvider.class);
    private static final int DEFAULT_TIMEOUT = 5_000; // 5 seconds in milliseconds
    private static final int DEFAULT_PRUNE_DIR_DEPTH = 4;


    public MantaConfigContextProvider() {
    }

    @Override
    public ConfigContext get() {
        /* We get a new copy of the system properties each time because they
         * may have been changed */
        final Properties properties = System.getProperties();
        properties.putIfAbsent(MapConfigContext.MANTA_RETRIES_KEY, 0);
        properties.putIfAbsent(MapConfigContext.MANTA_TIMEOUT_KEY, DEFAULT_TIMEOUT);
        properties.putIfAbsent(MapConfigContext.MANTA_TCP_SOCKET_TIMEOUT_KEY, DEFAULT_TIMEOUT);
        properties.putIfAbsent(MapConfigContext.MANTA_PRUNE_EMPTY_PARENT_DEPTH_KEY, DEFAULT_PRUNE_DIR_DEPTH);

        /* We explicitly pass the sysprops to the constructor, so that our
         * defaults will be applied. */
        final ConfigContext config = new SystemSettingsConfigContext(true, properties);

        LOG.debug("Manta configuration: {}", config);

        validate(config);

        LOG.info("Request timeout is {} ms", config.getTimeout());

        return config;
    }

    /**
     * Validates that the passed {@link ConfigContext} instance is valid for
     * use with Manta Monitor.
     *
     * @param config instance to validate
     */
    private static void validate(final ConfigContext config) {
        final MetricReporterMode configuredMode = config.getMetricReporterMode();

        if (!MetricReporterMode.JMX.equals(configuredMode)) {
            String msg = "Metric reporter mode must be set to JMX for "
                    + "Manta Monitor to operate correctly. Actual setting: %s\n";
            System.err.printf(msg, configuredMode);
            System.exit(-1);
        }

        try {
            ConfigContext.validate(config);
        } catch (ConfigurationException e) {
            final String msg = "Settings required for the correct "
                    + "functioning of Java Manta SDK were not set. Please refer "
                    + "to: https://github.com/joyent/java-manta/blob/master/USAGE.md#parameters";
            System.err.println(msg);
            System.err.println(e);
            System.exit(1);
        }
    }
}
