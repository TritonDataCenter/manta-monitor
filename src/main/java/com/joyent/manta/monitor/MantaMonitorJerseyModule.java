/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.servlet.ServletModule;
import io.logz.guice.jersey.JettyServerCreator;
import io.logz.guice.jersey.configuration.JerseyConfiguration;
import java.util.Objects;
import org.eclipse.jetty.server.Server;

/**
 * This implementation of {@link AbstractModule} provides dependency injection for the creating and configuring
 * {@link MantaMonitorJerseyServer}.
 */

public class MantaMonitorJerseyModule extends AbstractModule {
    private final JerseyConfiguration jerseyConfiguration;
    private final JettyServerCreator jettyServerCreator;

    public MantaMonitorJerseyModule(final JerseyConfiguration jerseyConfiguration) {
        this(jerseyConfiguration, Server::new);
    }

    public MantaMonitorJerseyModule(final JerseyConfiguration jerseyConfiguration, final JettyServerCreator jettyServerCreator) {
        this.jerseyConfiguration = (JerseyConfiguration)Objects.requireNonNull(jerseyConfiguration);
        this.jettyServerCreator = (JettyServerCreator)Objects.requireNonNull(jettyServerCreator);
    }

    protected void configure() {
        Provider<Injector> injectorProvider = this.getProvider(Injector.class);
        this.install(new ServletModule());
        this.bind(MantaMonitorJerseyServer.class).toInstance(new MantaMonitorJerseyServer(this.jerseyConfiguration,
                injectorProvider::get, this.jettyServerCreator));
        this.bind(JerseyConfiguration.class).toInstance(this.jerseyConfiguration);
    }
}
