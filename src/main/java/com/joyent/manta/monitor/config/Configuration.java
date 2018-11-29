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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration class that maps to a JSON configuration file.
 */
public class Configuration {
    private final Set<Runner> testRunners;
    private final AtomicInteger jettyServerPort = new AtomicInteger();

    @JsonCreator
    public Configuration(@JsonProperty("testRunners") final Set<Runner> testRunners,
                         @JsonProperty("jettyServerPort") final int jettyServerPort) {

        this.testRunners = ImmutableSet.copyOf(testRunners);
        this.jettyServerPort.set(jettyServerPort);
    }

    public Set<Runner> getTestRunners() {
        return testRunners;
    }

    public int getJettyServerPort() {
        return jettyServerPort.get();
    }
}
