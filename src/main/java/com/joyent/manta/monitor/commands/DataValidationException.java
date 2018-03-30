/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.commands;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.exception.ExceptionContext;

/**
 * Exception thrown when data in Manta does not match expectations.
 */
public class DataValidationException extends ContextedRuntimeException {
    public DataValidationException() {
    }

    public DataValidationException(final String message) {
        super(message);
    }

    public DataValidationException(final Throwable cause) {
        super(cause);
    }

    public DataValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public DataValidationException(final String message, final Throwable cause,
                                   final ExceptionContext context) {
        super(message, cause, context);
    }
}
