/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.exception.ExceptionContext;

/**
 * An exception class that provides additional contexts to the errors arising out of the failure to extract JMX monitoring metrics
 * from the platform mbean server.
 */
public class MBeanServerOperationException extends ContextedRuntimeException {
    private static final long serialVersionUID = -6546745118192162462L;

    public MBeanServerOperationException() {
    }

    public MBeanServerOperationException(final String message) {
        super(message);
    }

    public MBeanServerOperationException(final Throwable cause) {
        super(cause);
    }

    public MBeanServerOperationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public MBeanServerOperationException(final String message, final Throwable cause, final ExceptionContext context) {
        super(message, cause, context);
    }
}
