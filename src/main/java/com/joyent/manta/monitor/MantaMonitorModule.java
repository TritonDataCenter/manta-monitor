/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.joyent.manta.client.MantaClient;
import io.honeybadger.reporter.HoneybadgerUncaughtExceptionHandler;
import io.honeybadger.reporter.NoticeReporter;

public class MantaMonitorModule implements Module {
    private final HoneybadgerUncaughtExceptionHandler honeyBadgerHandler;

    public MantaMonitorModule(final HoneybadgerUncaughtExceptionHandler honeyBadgerHandler) {
        this.honeyBadgerHandler = honeyBadgerHandler;
    }

    public void configure(final Binder binder) {
        binder.bind(io.honeybadger.reporter.config.ConfigContext.class).toInstance(honeyBadgerHandler.getConfig());
        binder.bind(NoticeReporter.class).toInstance(honeyBadgerHandler.getReporter());
        binder.bind(Thread.UncaughtExceptionHandler.class).toInstance(honeyBadgerHandler);
        binder.bind(com.joyent.manta.config.ConfigContext.class).toProvider(MantaConfigContextProvider.class);
        binder.bind(MantaClient.class).toProvider(MantaClientProvider.class).asEagerSingleton();
    }
}
