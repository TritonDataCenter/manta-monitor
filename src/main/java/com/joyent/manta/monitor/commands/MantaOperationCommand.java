/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.commands;

import com.joyent.manta.monitor.MantaOperationContext;
import org.apache.commons.chain.Context;
import org.apache.commons.chain.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface in which all Manta specific commands inherit from. This interface
 * extends the {@link Filter} API such that it natively supports
 * {@link MantaOperationContext} without additional casting.
 */
public interface MantaOperationCommand extends Filter {
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

    @SuppressWarnings("unchecked")
    @Override
    default boolean postprocess(Context context, Exception exception) {
        if (exception == null) {
            return CONTINUE_PROCESSING;
        }

        context.put(MantaOperationContext.EXCEPTION_KEY, exception);

        return PROCESSING_COMPLETE;
    }
}
