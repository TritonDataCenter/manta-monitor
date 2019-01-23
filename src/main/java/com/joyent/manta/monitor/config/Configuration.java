/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Configuration class that maps to a JSON configuration file.
 */
public class Configuration {
    private final Set<Runner> testRunners;

    @JsonCreator
    public Configuration(@JsonProperty("testRunners") final Set<Runner> testRunners) {
        this.testRunners = ImmutableSet.copyOf(testRunners);
    }

    public Set<Runner> getTestRunners() {
        return testRunners;
    }
}
