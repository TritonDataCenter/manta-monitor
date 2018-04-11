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
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

/**
 * Configuration class for describing test runners.
 */
public class Runner {
    private String chainClassName;
    private String name;
    private int threads;
    private int minFileSize;
    private int maxFileSize;

    @JsonCreator
    public Runner(@JsonProperty("chainClassName") final String chainClassName,
                  @JsonProperty("name") final String name,
                  @JsonProperty("threads") final int threads,
                  @JsonProperty("minFileSize") final int minFileSize,
                  @JsonProperty("maxFileSize") final int maxFileSize) {
        this.chainClassName = chainClassName;
        this.name = name;
        this.threads = threads;
        this.minFileSize = minFileSize;
        this.maxFileSize = maxFileSize;
    }

    public String getChainClassName() {
        return chainClassName;
    }

    public Runner setChainClassName(final String chainClassName) {
        this.chainClassName = chainClassName;
        return this;
    }

    public String getName() {
        return name;
    }

    public Runner setName(final String name) {
        this.name = name;
        return this;
    }

    public int getThreads() {
        return threads;
    }

    public Runner setThreads(final int threads) {
        this.threads = threads;
        return this;
    }

    public int getMinFileSize() {
        return minFileSize;
    }

    public Runner setMinFileSize(final int minFileSize) {
        this.minFileSize = minFileSize;
        return this;
    }

    public int getMaxFileSize() {
        return maxFileSize;
    }

    public Runner setMaxFileSize(final int maxFileSize) {
        this.maxFileSize = maxFileSize;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Runner runner = (Runner) o;
        return threads == runner.threads
                && Objects.equals(chainClassName, runner.chainClassName)
                && Objects.equals(name, runner.name)
                && Objects.equals(minFileSize, runner.minFileSize)
                && Objects.equals(maxFileSize, runner.maxFileSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chainClassName, name, threads, minFileSize, maxFileSize);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("chainClassName", chainClassName)
                .append("name", name)
                .append("threads", threads)
                .append("minFileSize", minFileSize)
                .append("maxFileSize", maxFileSize)
                .toString();
    }
}
