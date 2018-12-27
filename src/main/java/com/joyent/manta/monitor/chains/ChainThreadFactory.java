/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.chains;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.LongAdder;

/**
 * This implementation of {@link ThreadFactory} creates threads for the running the {@link MantaOperationsChain}.
 */
public class ChainThreadFactory implements ThreadFactory {
    private final LongAdder count = new LongAdder();
    private final ThreadGroup threadGroup;
    private final String threadPrefix;
    private final Thread.UncaughtExceptionHandler exceptionHandler;

    public ChainThreadFactory(final ThreadGroup threadGroup,
                              final String threadPrefix,
                              final Thread.UncaughtExceptionHandler exceptionHandler) {
        this.threadGroup = threadGroup;
        this.threadPrefix = threadPrefix;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public Thread newThread(final Runnable r) {
        count.increment();
        final String name = String.format("%s-%d", threadPrefix,
                count.sum());
        final Thread thread = new Thread(threadGroup, r, name);
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(exceptionHandler);

        return thread;
    }
}
