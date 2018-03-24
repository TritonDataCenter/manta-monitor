package com.joyent.manta.monitor.chains;

import com.joyent.manta.monitor.commands.MantaOperationCommand;
import com.joyent.manta.monitor.MantaOperationContext;
import io.honeybadger.reporter.HoneybadgerUncaughtExceptionHandler;
import org.apache.commons.chain.impl.ChainBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class MantaOperationsChain extends ChainBase {
    private static final Logger LOG = LoggerFactory.getLogger(MantaOperationsChain.class);

    private final Thread.UncaughtExceptionHandler exceptionHandler;

    public MantaOperationsChain(final Collection<? super MantaOperationCommand> commands,
                                final Thread.UncaughtExceptionHandler exceptionHandler) {
        super(commands);
        this.exceptionHandler = exceptionHandler;
    }

    public void execute(final MantaOperationContext context) {
        try {
            super.execute(context);
        } catch (Exception e) {
            try {
                exceptionHandler.uncaughtException(Thread.currentThread(), e);
            } catch (RuntimeException re) {
                LOG.error("Error logging exception to Honeybadger.io", re);
            }

            LOG.error("Error running operation", e);
        }
    }
}
