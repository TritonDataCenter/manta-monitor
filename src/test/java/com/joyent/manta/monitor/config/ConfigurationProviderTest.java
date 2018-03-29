/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.config;

import com.google.common.collect.ImmutableSet;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Provider;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

@Test
public class ConfigurationProviderTest {
    public void canReadFromConfigFile() throws URISyntaxException {
        ClassLoader classLoader = this.getClass().getClassLoader();
        URI configUri = classLoader.getResource("test-configuration.json").toURI();
        Provider<Configuration> instance = new ConfigurationProvider(null, configUri);


        Set<Runner> expected = ImmutableSet.of(
                new Runner("com.joyent.manta.monitor.chains.FileUploadGetDeleteChain",
                        "simple-put", 5)
        );
        Configuration config = instance.get();
        Assert.assertNotNull(config);
        Assert.assertEquals(config.getTestRunners(), expected);
    }
}
