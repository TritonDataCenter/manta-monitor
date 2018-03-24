package com.joyent.manta.monitor.chains;

import io.honeybadger.reporter.HoneybadgerUncaughtExceptionHandler;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.LongAdder;

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
