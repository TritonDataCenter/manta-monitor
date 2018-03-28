package com.joyent.manta.monitor;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.joyent.manta.client.MantaClient;
import io.honeybadger.reporter.HoneybadgerUncaughtExceptionHandler;
import io.honeybadger.reporter.NoticeReporter;
import io.honeybadger.reporter.config.ConfigContext;

public class MonitorModule implements Module {
    private final HoneybadgerUncaughtExceptionHandler honeyBadgerHandler;

    public MonitorModule(final HoneybadgerUncaughtExceptionHandler honeyBadgerHandler) {
        this.honeyBadgerHandler = honeyBadgerHandler;
    }

    public void configure(final Binder binder) {
        binder.bind(ConfigContext.class).toInstance(honeyBadgerHandler.getConfig());
        binder.bind(NoticeReporter.class).toInstance(honeyBadgerHandler.getReporter());
        binder.bind(Thread.UncaughtExceptionHandler.class).toInstance(honeyBadgerHandler);;
        binder.bind(MantaClient.class).toProvider(MantaClientProvider.class);
    }
}
