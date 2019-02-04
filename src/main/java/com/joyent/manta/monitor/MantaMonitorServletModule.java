/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.google.inject.servlet.ServletModule;
import io.prometheus.client.exporter.MetricsServlet;

/**
 * Module used by {@link JettyServerBuilderModule} to inject dependencies for the
 * metric servlet.
 */
public class MantaMonitorServletModule extends ServletModule {

    @Override
    protected void configureServlets() {
        bind(MetricsServlet.class).asEagerSingleton();

        serve("/metrics").with(MetricsServlet.class);
    }
}
