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
import com.joyent.manta.monitor.MantaOperationContext;
import com.joyent.manta.monitor.MantaOperationException;
import com.joyent.manta.monitor.commands.MantaOperationCommand;
import io.honeybadger.reporter.NoticeReporter;
import io.honeybadger.reporter.dto.Request;
import org.apache.commons.chain.impl.ChainBase;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;

public class MantaOperationsChain extends ChainBase {
    private static final Logger LOG = LoggerFactory.getLogger(MantaOperationsChain.class);
    private final NoticeReporter reporter;
    private final HoneyBadgerRequestFactory requestFactory;

    public MantaOperationsChain(final Collection<? super MantaOperationCommand> commands,
                                final NoticeReporter reporter,
                                final HoneyBadgerRequestFactory requestFactory) {
        super(commands);
        this.reporter = reporter;
        this.requestFactory = requestFactory;
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

        Request request = null;
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
        } else if (path != null) {
            request = requestFactory.build(path);
        }

        reportAndLog(exception, request);
    }

    private void reportAndLog(final Throwable throwable, final Request request) {
        try {
            reporter.reportError(throwable, request);
        } catch (RuntimeException re) {
            LOG.error("Error logging exception to Honeybadger.io", re);
        }

        LOG.error("Error running operation", throwable);
    }
}
