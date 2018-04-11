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
import com.joyent.manta.config.StandardConfigContext;
import com.joyent.manta.config.SystemSettingsConfigContext;
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

    private final StandardConfigContext appDefaults = new StandardConfigContext();

    {
        appDefaults.setRetries(0)
                   .setTimeout(DEFAULT_TIMEOUT)
                   .setTcpSocketTimeout(DEFAULT_TIMEOUT);
    }

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

        /* We explicitly pass the sysprops to the constructor, so that our
         * defaults will be applied. */
        final ConfigContext config = new SystemSettingsConfigContext(true, properties);

        LOG.debug("Manta configuration: {}", config);
        LOG.info("Request timeout is {} ms", config.getTimeout());

        return config;
    }
}
