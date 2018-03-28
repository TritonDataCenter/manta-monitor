/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.chains;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.monitor.MantaOperationContext;
import com.joyent.manta.monitor.functions.GeneratePathBasedOnSHA256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.joyent.manta.client.MantaClient.SEPARATOR;

public class ChainRunner {
    private static final Logger LOG = LoggerFactory.getLogger(ChainRunner.class);

    private final MantaOperationsChain chain;
    private final String name;
    private final int threads;
    private final MantaClient client;
    private final ExecutorService executorService;
    private final GeneratePathBasedOnSHA256 pathGenerator;

    private volatile boolean running = true;

    public ChainRunner(final MantaOperationsChain chain,
                       final String name,
                       final int threads,
                       final MantaClient client,
                       final Thread.UncaughtExceptionHandler exceptionHandler) {
        this.chain = chain;
        this.name = name;
        this.threads = threads;
        this.client = client;
        this.pathGenerator = new GeneratePathBasedOnSHA256(
                client.getContext().getMantaHomeDirectory()
                        + SEPARATOR + "stor" + SEPARATOR + "manta-monitor-data");

        final ThreadGroup threadGroup = new ThreadGroup(name);
        threadGroup.setDaemon(true);

         final ThreadFactory threadFactory = new ChainThreadFactory(
                 threadGroup, name, exceptionHandler);

        this.executorService = Executors.newFixedThreadPool(
                threads, threadFactory);
    }

    public void start() {
        if (!running) {
            throw new IllegalArgumentException("This runner has already been "
                    + "stopped - restart unsupported");
        }

        LOG.info("Starting {} threads to run [{}]", threads, name);

        final Callable<Void> callable = () -> {
            final MantaOperationContext context = new MantaOperationContext();

            while (running) {
                resetContext(context);
                chain.execute(context);
            }

            return null;
        };

        try {
            executorService.invokeAll(Collections.nCopies(threads, callable));
        } catch (InterruptedException e) {
            this.running = false;
            executorService.shutdown();
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        this.running = false;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public boolean isRunning() {
        return running;
    }

    private void resetContext(MantaOperationContext context) {
        context.clear();

        context.setMantaClient(client)
               .setFilePathGenerationFunction(pathGenerator)
               .setMinFileSize(100)
               .setMaxFileSize(10000);
    }
}
