/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.joyent.manta.monitor.config.Configuration;
import io.logz.guice.jersey.configuration.JerseyConfiguration;

/**
 * Class that provides {@link MantaMonitorModule} dependencies to the {@link MantaMonitorServletModule}.
 */
public class JettyServerBuilderModule implements Module {
    private final int jettyServerPort;

    public JettyServerBuilderModule(final Injector applicationInjector) {
        this.jettyServerPort = applicationInjector.getInstance(Configuration.class).getJettyServerPort();
    }

    @Override
    public void configure(final Binder binder) {
        final JerseyConfiguration jerseyConfig = JerseyConfiguration.builder()
                .addPort(jettyServerPort)
                .build();

        binder.install(new MantaMonitorJerseyModule(jerseyConfig));
    }
}
