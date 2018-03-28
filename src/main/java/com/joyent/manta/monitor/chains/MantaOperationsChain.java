/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.chains;

import com.joyent.manta.exception.MantaClientHttpResponseException;
import com.joyent.manta.monitor.HoneyBadgerRequestFactory;
import com.joyent.manta.monitor.MantaOperationContext;
import com.joyent.manta.monitor.MantaOperationException;
import com.joyent.manta.monitor.commands.MantaOperationCommand;
import io.honeybadger.reporter.NoticeReporter;
import io.honeybadger.reporter.dto.Request;
import org.apache.commons.chain.impl.ChainBase;
import org.apache.commons.lang3.exception.ExceptionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        try {
            super.execute(context);
        } catch (MantaOperationException e) {
            final Throwable cause = e.getCause();
            final Request request;

            if (cause instanceof MantaClientHttpResponseException) {
                MantaClientHttpResponseException responseException = (MantaClientHttpResponseException)cause;
                request = requestFactory.build(e.getPath(), responseException.getHeaders(), responseException);
            } else if (cause instanceof ExceptionContext) {
                ExceptionContext exceptionContext = (ExceptionContext)cause;
                request = requestFactory.build(e.getPath(), null, exceptionContext);
            } else {
                request = requestFactory.build(e.getPath());
            }

            reportAndLog(cause, request);
        } catch (Exception e) {
            reportAndLog(e, null);
        }
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
