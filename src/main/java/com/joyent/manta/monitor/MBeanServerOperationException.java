package com.joyent.manta.monitor;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.exception.ExceptionContext;

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
