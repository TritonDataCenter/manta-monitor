/*
 * Copyright (c) 2019, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.exception.ExceptionContext;

/**
 * An exception class that provides additional contexts to the errors arising out of the failure to configure the
 * embedded Jersey Server from the given env variables.
 */
public class MantaMonitorJerseyServerException extends ContextedRuntimeException {
    private static final long serialVersionUID = -8348174030695591131L;

    public MantaMonitorJerseyServerException() { }

    public MantaMonitorJerseyServerException(final String message) {
        super(message);
    }

    public MantaMonitorJerseyServerException(final Throwable cause) {
        super(cause);
    }

    public MantaMonitorJerseyServerException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public MantaMonitorJerseyServerException(final String message,
                                             final Throwable cause,
                                             final ExceptionContext exceptionContext) {
        super(message, cause, exceptionContext);
    }
}
