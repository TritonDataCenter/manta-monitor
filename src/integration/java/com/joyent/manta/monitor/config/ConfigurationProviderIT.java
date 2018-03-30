/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.config;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import com.joyent.manta.client.MantaClient;
import com.joyent.manta.config.MapConfigContext;
import com.joyent.manta.monitor.MantaMonitorModule;
import io.honeybadger.reporter.NoticeReporter;
import io.honeybadger.reporter.config.StandardConfigContext;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;

@Test
public class ConfigurationProviderIT {
    private static final String ROOT_PATH_TEMPLATE =
            "%s/stor/java-manta-integration-tests/manta-monitor";

    private MantaClient client;
    private String rootPath;

    @BeforeTest
    public void before() {
        // This is set manually, so that we can work around this bug:
        // https://github.com/joyent/java-manta/issues/407
        System.setProperty(MapConfigContext.MANTA_RETRIES_KEY, "3");

        NoticeReporter reporter = Mockito.mock(NoticeReporter.class);
        when(reporter.getConfig()).thenReturn(new StandardConfigContext());

        Thread.UncaughtExceptionHandler exceptionHandler =
                Thread.currentThread().getUncaughtExceptionHandler();

        MantaMonitorModule module = new MantaMonitorModule(exceptionHandler, reporter,
                URI.create("file:///"));
        Injector injector = com.google.inject.Guice.createInjector(module);
        this.client = injector.getInstance(MantaClient.class);
        this.rootPath = String.format(ROOT_PATH_TEMPLATE,
                client.getContext().getMantaHomeDirectory());
    }

    @AfterTest
    public void after() throws IOException {
        try {
            client.deleteRecursive(rootPath);
        } finally {
            client.closeWithWarning();
        }
    }

    public void canLoadConfigFromManta() throws IOException {
        String pathTemplate = "%s/%s";
        String pathString = String.format(pathTemplate,
                rootPath, UUID.randomUUID());

        client.putDirectory(pathString, true);

        String remoteFile = pathString + "/config-test.json";

        URI configUri = URI.create("manta://" + remoteFile);

        ClassLoader classLoader = this.getClass().getClassLoader();
        try (InputStream in = classLoader.getResourceAsStream("test-configuration.json")) {
            client.put(remoteFile, in);
        }

        Set<Runner> expected = ImmutableSet.of(
                new Runner("com.joyent.manta.monitor.chains.FileUploadGetDeleteChain",
                        "simple-put", 5, 4096, 65536)
        );

        // Note we don't load the ConfigurationProvider available via DI
        // because we want to control its state manually
        Provider<Configuration> instance = new ConfigurationProvider(client, configUri);
        Configuration config = instance.get();
        Assert.assertNotNull(config);
        Assert.assertEquals(config.getTestRunners(), expected);
    }
}
