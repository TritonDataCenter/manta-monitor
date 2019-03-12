/*
 * Copyright (c) 2019, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

import javax.inject.Inject;

public class JerseyGuiceServletContextListener extends GuiceServletContextListener {
    private final Injector injector;

    @Inject
    public JerseyGuiceServletContextListener(final Injector injector) {
        this.injector = injector;
    }
    @Override
    protected Injector getInjector() {
        return injector;
    }
}
