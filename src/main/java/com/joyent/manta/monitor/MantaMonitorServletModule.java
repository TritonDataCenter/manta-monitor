package com.joyent.manta.monitor;

import com.google.inject.servlet.ServletModule;
import com.joyent.manta.monitor.servlets.MantaMonitorServlet;
import io.prometheus.client.exporter.MetricsServlet;

public class MantaMonitorServletModule extends ServletModule {

    @Override
    protected void configureServlets() {
        bind(MantaMonitorServlet.class);
        bind(MetricsServlet.class).asEagerSingleton();

        serve("/monitor").with(MantaMonitorServlet.class);

        serve("/metrics").with(MetricsServlet.class);
    }
}
