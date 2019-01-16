/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.config.ConfigContext;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Provides a new instance of {@link MantaClient} that is configured with the
 * relevant defaults for this application.
 */
public class MantaClientProvider implements Provider<MantaClient> {
    private final Provider<ConfigContext> configProvider;

    @Inject
    public MantaClientProvider(final Provider<ConfigContext> configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public MantaClient get() {
        return new MantaClient(configProvider.get());
    }
}
