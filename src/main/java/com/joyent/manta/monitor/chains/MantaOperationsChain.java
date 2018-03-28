package com.joyent.manta.monitor.chains;

import com.joyent.manta.monitor.HoneyBadgerRequestFactory;
import com.joyent.manta.monitor.MantaOperationException;
import com.joyent.manta.monitor.commands.MantaOperationCommand;
import com.joyent.manta.monitor.MantaOperationContext;
import io.honeybadger.reporter.HoneybadgerUncaughtExceptionHandler;
import io.honeybadger.reporter.NoticeReporter;
import io.honeybadger.reporter.dto.Request;
import org.apache.commons.chain.impl.ChainBase;
import org.apache.commons.lang3.exception.ExceptionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

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

            if (cause instanceof ExceptionContext) {
                ExceptionContext contexted = (ExceptionContext)cause;
                request = requestFactory.build(e.getPath(), contexted);
            } else {
                request = requestFactory.build(e.getPath(), e);
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
