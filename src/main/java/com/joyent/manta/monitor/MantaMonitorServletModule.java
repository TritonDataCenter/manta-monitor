/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.google.inject.servlet.ServletModule;
import com.joyent.manta.monitor.servlets.MantaMonitorServlet;
import io.prometheus.client.exporter.MetricsServlet;

/**
 * Module used by {@link JettyServerBuilderModule} to inject dependencies for the metric and monitor servlets.
 */
public class MantaMonitorServletModule extends ServletModule {

    @Override
    protected void configureServlets() {
        bind(MantaMonitorServlet.class);
        bind(MetricsServlet.class).asEagerSingleton();

        serve("/monitor").with(MantaMonitorServlet.class);

        serve("/metrics").with(MetricsServlet.class);
    }
}
