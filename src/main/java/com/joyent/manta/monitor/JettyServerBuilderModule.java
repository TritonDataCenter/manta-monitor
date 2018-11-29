package com.joyent.manta.monitor;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.joyent.manta.monitor.config.Configuration;
import io.logz.guice.jersey.JerseyModule;
import io.logz.guice.jersey.configuration.JerseyConfiguration;

public class JettyServerBuilderModule implements Module {
    private final int jettyServerPort;

    public JettyServerBuilderModule(Injector applicationInjector) {
        this.jettyServerPort = applicationInjector.getInstance(Configuration.class).getJettyServerPort();
    }

    @Override
    public void configure(final Binder binder) {
        final JerseyConfiguration jerseyConfig = JerseyConfiguration.builder()
                .addPort(jettyServerPort)
                .build();

        binder.install(new JerseyModule(jerseyConfig));
    }
}
