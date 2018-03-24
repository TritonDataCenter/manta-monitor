package com.joyent.manta.monitor.commands;

import com.joyent.manta.monitor.MantaOperationContext;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface MantaOperationCommand extends Command {
    Logger LOG = LoggerFactory.getLogger(MantaOperationCommand.class);

    @Override
    default boolean execute(Context context) throws Exception {
        if (!context.getClass().equals(MantaOperationContext.class)) {
            throw new IllegalArgumentException("Unexpected context class: "
                + context.getClass().getName());
        }

        LOG.trace("Executing command: [{}]", this.getClass().getSimpleName());

        return execute((MantaOperationContext)context);
    }

    boolean execute(MantaOperationContext context) throws Exception;
}
