package com.joyent.manta.monitor;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.joyent.manta.client.MantaClient;

public class MonitorModule implements Module {
    public void configure(final Binder binder) {
        binder.bind(MantaClient.class).toProvider(MantaClientProvider.class);

    }
}
