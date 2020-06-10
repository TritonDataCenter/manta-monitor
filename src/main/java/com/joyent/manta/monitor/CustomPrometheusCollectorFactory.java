/*
 * Copyright (c) 2019, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

/**
 * Interface required by {@link com.google.inject.assistedinject.FactoryModuleBuilder}
 * to create the {@link CustomPrometheusCollector}.
 */
public interface CustomPrometheusCollectorFactory {

    CustomPrometheusCollector create(String testType);
}
