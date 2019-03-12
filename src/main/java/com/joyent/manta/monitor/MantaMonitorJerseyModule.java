/*
 * Copyright (c) 2019, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceServletContextListener;
import io.logz.guice.jersey.JettyServerCreator;
import org.eclipse.jetty.server.Server;

import java.util.Objects;

/**
 * This implementation of {@link AbstractModule} provides dependency injection
 * for the creating and configuring {@link MantaMonitorJerseyServer}.
 */

public class MantaMonitorJerseyModule extends AbstractModule {
    private final JettyServerCreator jettyServerCreator;

    public MantaMonitorJerseyModule() {
        this(Server::new);
    }

    public MantaMonitorJerseyModule(final JettyServerCreator jettyServerCreator) {
        this.jettyServerCreator = Objects.requireNonNull(jettyServerCreator);
    }

    protected void configure() {
        this.bind(JettyServerCreator.class).toInstance(this.jettyServerCreator);
        this.bind(MantaMonitorJerseyServer.class).asEagerSingleton();
        this.bind(GuiceServletContextListener.class).to(JerseyGuiceServletContextListener.class)
                .in(Singleton.class);
    }
}
