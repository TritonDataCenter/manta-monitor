package com.joyent.manta.monitor;

import io.prometheus.client.Counter;

import javax.inject.Provider;

public class SharedRequestCounterProvider implements Provider<Counter> {
    @Override
    public Counter get() {
        return Counter.build()
                .name("monitor")
                .help("Request Counter")
                .labelNames("method")
                .register();
    }
}
