/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.config;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.exception.ExceptionContext;

/**
 * Exception thrown when there is a problem loading a configuration file.
 */
public class ConfigurationLoadException extends ContextedRuntimeException {
    public ConfigurationLoadException() {
    }

    public ConfigurationLoadException(final String message) {
        super(message);
    }

    public ConfigurationLoadException(final Throwable cause) {
        super(cause);
    }

    public ConfigurationLoadException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ConfigurationLoadException(final String message, final Throwable cause,
                                      final ExceptionContext context) {
        super(message, cause, context);
    }
}
