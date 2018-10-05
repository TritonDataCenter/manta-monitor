package com.joyent.manta.monitor;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.joyent.manta.monitor.jetty.MantaMonitorServlet;
import io.prometheus.client.Counter;

import javax.inject.Named;

public class MantaMonitorServletModule extends ServletModule {

    @Provides
    @Singleton
    @Named("SharedCounter")
    Counter provideCounter() {
        return Counter.build()
                .name("monitor")
                .help("Elapsed Time.")
                .labelNames("method")
                .register();
    }

    @Override
    protected void configureServlets() {
        bind(MantaMonitorServlet.class);

        serve("/monitor").with(MantaMonitorServlet.class);

    }
}
