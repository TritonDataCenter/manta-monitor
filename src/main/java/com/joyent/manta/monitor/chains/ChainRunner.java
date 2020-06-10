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
import com.joyent.manta.monitor.config.Runner;
import com.joyent.manta.monitor.functions.GeneratePathBasedOnSHA256;
import io.prometheus.client.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

import static com.joyent.manta.client.MantaClient.SEPARATOR;

/**
 * Class that executes the {@link MantaOperationsChain} threads.
 */
public class ChainRunner {
    private static final Logger LOG = LoggerFactory.getLogger(ChainRunner.class);

    private final MantaOperationsChain chain;
    private final String name;
    private final int threads;
    private final MantaClient client;
    private final ExecutorService executorService;
    private final Runner runnerConfig;
    private final String testType;

    private volatile boolean running = true;

    private final Map<String, Histogram> requestPutHistogramsMap;

    public ChainRunner(final MantaOperationsChain chain,
                       final Runner runnerConfig,
                       final MantaClient client,
                       final Thread.UncaughtExceptionHandler exceptionHandler,
                       final Map<String, Histogram> requestPutHistogramsMap,
                       final String testType) {
        this.chain = chain;
        this.name = runnerConfig.getName();
        this.threads = runnerConfig.getThreads();
        this.client = client;
        this.runnerConfig = runnerConfig;
        this.requestPutHistogramsMap = requestPutHistogramsMap;
        this.testType = testType;

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
            final String baseDirOrBucket = buildBaseDirOrBucket();
            final Function<byte[], String> pathGenerator = new GeneratePathBasedOnSHA256(baseDirOrBucket);

            while (running) {
                final MantaOperationContext context = new MantaOperationContext()
                        .setMantaClient(client)
                        .setFilePathGenerationFunction(pathGenerator)
                        .setMinFileSize(runnerConfig.getMinFileSize())
                        .setMaxFileSize(runnerConfig.getMaxFileSize())
                        .setChainClassNameKey(chain.getClass().getSimpleName())
                        .setRequestPutHistograms(requestPutHistogramsMap)
                        .setTestBaseDirOrBucket(baseDirOrBucket)
                        .setTestType(testType);

                chain.execute(context);
            }

            return null;
        };

        // Start up multiple testing threads so that we reach the number in
        // the configuration
        for (int i = 0; i < threads; i++) {
            executorService.submit(callable);
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

    private String buildBaseDirOrBucket() {
        if ("buckets".equals(testType)) {
            return client.getContext().getMantaBucketsDirectory()
                    + SEPARATOR + "manta-monitor-bucket";
        }
        return client.getContext().getMantaHomeDirectory()
                + SEPARATOR + "stor" + SEPARATOR + "manta-monitor-data"
                + SEPARATOR + chain.getClass().getSimpleName();
    }
}
