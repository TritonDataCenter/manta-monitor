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
 * An exception class that provides additional contexts to the errors arising out of the failure to connect and
 * perform operations on the manta client.
 */
public class MantaOperationException extends ContextedRuntimeException {
    private static final long serialVersionUID = -1269054951271990255L;

    public MantaOperationException() {
    }

    public MantaOperationException(final String message) {
        super(message);
    }

    public MantaOperationException(final Throwable cause) {
        super(cause);
    }

    public MantaOperationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public MantaOperationException(final String message, final Throwable cause, final ExceptionContext context) {
        super(message, cause, context);
    }

    public ContextedRuntimeException setPath(final String path) {
        super.addContextValue("path", path);
        return this;
    }

    public String getPath() {
        return super.getFirstContextValue("path").toString();
    }
}
