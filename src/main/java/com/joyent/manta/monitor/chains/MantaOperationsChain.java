/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.chains;

import com.joyent.manta.exception.MantaClientHttpResponseException;
import com.joyent.manta.http.MantaHttpHeaders;
import com.joyent.manta.monitor.HoneyBadgerRequestFactory;
import com.joyent.manta.monitor.InstanceMetadata;
import com.joyent.manta.monitor.MantaOperationContext;
import com.joyent.manta.monitor.MantaOperationException;
import com.joyent.manta.monitor.commands.MantaOperationCommand;
import io.honeybadger.reporter.NoticeReporter;
import io.honeybadger.reporter.dto.Request;
import org.apache.commons.chain.impl.ChainBase;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionContext;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class MantaOperationsChain extends ChainBase {
    private static final Logger LOG = LoggerFactory.getLogger(MantaOperationsChain.class);
    private final NoticeReporter reporter;
    private final HoneyBadgerRequestFactory requestFactory;
    private final InstanceMetadata metadata;


    public MantaOperationsChain(final Collection<? super MantaOperationCommand> commands,
                                final NoticeReporter reporter,
                                final HoneyBadgerRequestFactory requestFactory,
                                final InstanceMetadata metadata) {
        super(commands);
        this.reporter = reporter;
        this.requestFactory = requestFactory;
        this.metadata = metadata;
    }

    public void execute(final MantaOperationContext context) {
        Throwable exception;

        try {
            LOG.info("{} starting", getClass().getSimpleName());
            super.execute(context);
            exception = context.getException();
        } catch (Exception e) {
            exception = e;
        } finally {
            LOG.info("{} finished", getClass().getSimpleName());
        }

        if (exception == null) {
            return;
        }

        Request request;
        String path = null;
        MantaHttpHeaders mantaHeaders = null;

        if (exception instanceof MantaOperationException) {
            final MantaOperationException moe = (MantaOperationException) exception;
            exception = moe.getCause();
            path = moe.getPath();
        }

        if (exception instanceof MantaClientHttpResponseException) {
            MantaClientHttpResponseException re = (MantaClientHttpResponseException)exception;
            mantaHeaders = re.getHeaders();
        }

        if (exception instanceof ExceptionContext) {
            ExceptionContext exceptionContext = (ExceptionContext) exception;

            if (path == null && exceptionContext.getContextLabels().contains("path")) {
                path = exceptionContext.getFirstContextValue("path").toString();
            } else if (path == null && exceptionContext.getContextLabels().contains("requestURL")) {
                final Object requestURL = exceptionContext.getFirstContextValue("requestURL");

                try {
                    URI uri = URI.create(requestURL.toString());
                    path = uri.getPath();
                } catch (IllegalArgumentException | NullPointerException uriE) {
                    LOG.error("Error parsing URI: {}", requestURL);
                }
            }

            String message = StringUtils.substringBefore(exception.getMessage(),
                    "Exception Context:");
            exceptionContext.setContextValue("actualMessage", message);
            request = requestFactory.build(path, mantaHeaders, exceptionContext);
        } else {
            request = requestFactory.build(path);
        }

        reportAndLog(exception, request);
    }

    private String extractMessageAndAddTags(final Throwable throwable, final Set<String> tags) {
        final Throwable rootCause = ExceptionUtils.getRootCause(throwable);

        if (rootCause != null && SocketTimeoutException.class.equals(rootCause.getClass())) {
            tags.add("socket-timeout");
            return rootCause.getMessage();
        }

        return throwable.getMessage();
    }

    private void reportAndLog(final Throwable throwable, final Request request) {
        final Set<String> tags = new LinkedHashSet<>(metadata.asTagSet());
        final String message = extractMessageAndAddTags(throwable, tags);

        try {
            reporter.reportError(throwable, request, message, tags);
        } catch (RuntimeException re) {
            LOG.error("Error logging exception to Honeybadger.io", re);
        }

        LOG.error("Error running operation", throwable);
    }
}
