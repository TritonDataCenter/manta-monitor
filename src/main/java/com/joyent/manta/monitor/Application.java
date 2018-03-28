/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.joyent.manta.client.MantaClient;
import com.joyent.manta.monitor.chains.ChainRunner;
import com.joyent.manta.monitor.chains.FileUploadGetDeleteChain;
import io.honeybadger.reporter.HoneybadgerUncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    private static final HoneybadgerUncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER;
    private static final MantaMonitorModule MONITOR_MODULE;

    static {
        UNCAUGHT_EXCEPTION_HANDLER = HoneybadgerUncaughtExceptionHandler.registerAsUncaughtExceptionHandler();
        MONITOR_MODULE = new MantaMonitorModule(UNCAUGHT_EXCEPTION_HANDLER);
    }

    public static void main(String[] args) throws InterruptedException {
        final Injector injector = Guice.createInjector(MONITOR_MODULE);
        final MantaClient client = injector.getInstance(MantaClient.class);
        final FileUploadGetDeleteChain chain = injector.getInstance(FileUploadGetDeleteChain.class);

        ChainRunner runner = new ChainRunner(chain, "simple-put",
                5, client, UNCAUGHT_EXCEPTION_HANDLER);

        runner.start();

        while (runner.isRunning()) {
            Thread.currentThread().wait();
        }
    }
}
