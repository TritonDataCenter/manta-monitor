/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.chains;

import com.google.common.collect.ImmutableMap;
import com.joyent.manta.exception.MantaClientHttpResponseException;
import com.joyent.manta.http.MantaHttpHeaders;
import com.joyent.manta.monitor.*;
import com.joyent.manta.monitor.commands.MantaOperationCommand;
import io.honeybadger.reporter.NoticeReporter;
import io.honeybadger.reporter.dto.Request;
import io.prometheus.client.CollectorRegistry;
import org.apache.commons.chain.impl.ChainBase;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionContext;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class MantaOperationsChain extends ChainBase {
    private static final Logger LOG = LoggerFactory.getLogger(MantaOperationsChain.class);
    private final NoticeReporter reporter;
    private final ThrowableProcessor throwableProcessor;
    private final InstanceMetadata metadata;
    private final Map<String, AtomicLong> clientStats;
    private final AtomicBoolean completionStatus = new AtomicBoolean(false);
    private final CustomPrometheusCollector customPrometheusCollector;

    private static final Map<Integer, String> STATUS_CODE_TO_TAG = ImmutableMap.of(
        HttpStatus.SC_INTERNAL_SERVER_ERROR, "internal-server-error",
        HttpStatus.SC_BAD_GATEWAY, "bad-gateway",
        HttpStatus.SC_GATEWAY_TIMEOUT, "gateway-timeout",
        HttpStatus.SC_INSUFFICIENT_STORAGE, "insufficient-storage",
        HttpStatus.SC_SERVICE_UNAVAILABLE, "service-unavailable"
    );

    @Inject
    public MantaOperationsChain(final Collection<? super MantaOperationCommand> commands,
                                final NoticeReporter reporter,
                                final HoneyBadgerRequestFactory requestFactory,
                                final InstanceMetadata metadata,
                                @Named("SharedStats") final Map<String, AtomicLong> clientStats,
                                final CustomPrometheusCollector customPrometheusCollector) {
        super(commands);
        this.reporter = reporter;
        this.metadata = metadata;
        this.throwableProcessor = new ThrowableProcessor(requestFactory);
        this.clientStats = clientStats;
        this.customPrometheusCollector= customPrometheusCollector;
    }

    public void execute(final MantaOperationContext context) {
        Throwable throwable;

        try {
            LOG.info("{} starting", getClass().getSimpleName());
            super.execute(context);
            throwable = context.getException();
            if(LOG.isInfoEnabled()) {
                LOG.info("Stopwatch recorded {} milliseconds", context.getStopWatch().elapsed(TimeUnit.MILLISECONDS));
                AtomicLong elapsedTime = new AtomicLong(context.getStopWatch().elapsed(TimeUnit.MILLISECONDS));
                clientStats.put(getClass().getSimpleName(), elapsedTime);
            }
            //Register the collector only once.
            if(completionStatus.compareAndSet(false, true)) {
                CollectorRegistry.defaultRegistry.register(customPrometheusCollector);
            }
        } catch (Exception e) {
            throwable = e;
        } finally {
            LOG.info("{} finished", getClass().getSimpleName());
        }

        if (throwable == null) {
            return;
        }

        final ThrowableProcessor.ProcessedResults results =
                throwableProcessor.process(throwable);

        reportAndLog(results);
    }

    private String extractMessageAndAddTags(final ThrowableProcessor.ProcessedResults results,
                                            final Set<String> tags) {
        final Throwable rootCause = results.rootCause;

        if (rootCause != null && SocketTimeoutException.class.equals(rootCause.getClass())) {
            tags.add("socket-timeout");
            return rootCause.getMessage();
        }

        for (Throwable t : results.throwableAndCauses) {
            if (t instanceof MantaClientHttpResponseException) {
                MantaClientHttpResponseException mchre = (MantaClientHttpResponseException)t;

                final int statusCode = mchre.getStatusCode();
                final String tag = STATUS_CODE_TO_TAG.get(statusCode);

                if (tag != null) {
                    tags.add(tag);
                }
            }
        }

        return parseMessage(results.throwable);
    }

    /**
     * Parses the exception message and strips out redundant context information
     * if we are already sending the information as part of the error context.
     *
     * @param throwable throwable to parse message from
     * @return string containing the throwable's error message
     */
    private static String parseMessage(final Throwable throwable) {
        if (throwable instanceof ExceptionContext) {
            final String msg = throwable.getMessage();

            return StringUtils.substringBefore(msg,
                    "Exception Context:").trim();
        } else {
            return throwable.getMessage();
        }
    }

    private void reportAndLog(final ThrowableProcessor.ProcessedResults results) {
        final Set<String> tags = new LinkedHashSet<>(metadata.asTagSet());
        final String message = extractMessageAndAddTags(results, tags);

        try {
            reporter.reportError(results.throwable, results.request, message, tags);
        } catch (RuntimeException re) {
            LOG.error("Error logging exception to Honeybadger.io", re);
        }

        LOG.error("Error running operation", results.throwable);
    }
}
