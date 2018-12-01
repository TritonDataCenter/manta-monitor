package com.joyent.manta.monitor;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.exception.ExceptionContext;

public class JettyServerOperationException extends ContextedRuntimeException {
    private static final long serialVersionUID = -3846558255935025714L;

    public JettyServerOperationException() {}

    public JettyServerOperationException(final String message) { super(message); }

    public JettyServerOperationException(final Throwable cause) { super(cause); }

    public JettyServerOperationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public JettyServerOperationException(final String message, final Throwable cause, final ExceptionContext context) {
        super(message, cause, context);
    }
}
