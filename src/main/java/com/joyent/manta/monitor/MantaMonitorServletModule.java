package com.joyent.manta.monitor;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.joyent.manta.monitor.servlets.MantaMonitorServlet;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.MetricsServlet;

import javax.inject.Named;

public class MantaMonitorServletModule extends ServletModule {

    @Provides
    @Singleton
    @Named("SharedCounter")
    Counter provideCounter() {
        return Counter.build()
                .name("monitor")
                .help("Request Counter")
                .labelNames("method")
                .register();
    }

    @Override
    protected void configureServlets() {
        bind(MantaMonitorServlet.class);
        bind(MetricsServlet.class).asEagerSingleton();

        serve("/monitor").with(MantaMonitorServlet.class);

        serve("/metrics").with(MetricsServlet.class);

    }
}
