/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaObjectMapper;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Provider class that provides configuration instances based on a URI.
 */
public class ConfigurationProvider implements Provider<Configuration> {
    private final MantaClient client;
    private final URI configFileUri;
    private final ObjectMapper mapper = MantaObjectMapper.INSTANCE
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .enable(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER);

    @Inject
    public ConfigurationProvider(@Nullable final MantaClient client,
                                 @Named("configUri") final URI configFileUri) {
        this.client = client;
        this.configFileUri = configFileUri;
    }

    @Override
    public Configuration get() {
        try (InputStream in = readFileFromURI()) {
            return mapper.readValue(in, Configuration.class);
        } catch (JsonMappingException e) {
            String msg = "Error parsing JSON data from configuration file";
            ConfigurationLoadException cle = new ConfigurationLoadException(msg, e);
            cle.setContextValue("uri", configFileUri.toASCIIString());

            throw cle;
        } catch (IOException e) {
            String msg = "Unable to read configuration file from stream";
            ConfigurationLoadException cle = new ConfigurationLoadException(msg, e);
            cle.setContextValue("uri", configFileUri.toASCIIString());

            throw cle;
        }
    }

    private InputStream readFileFromURI() throws IOException {
        if ("manta".equals(configFileUri.getScheme())) {
            if (client == null) {
                String msg = "Can't load file from Manta when MantaClient is null";
                ConfigurationLoadException cle = new ConfigurationLoadException(msg);
                cle.setContextValue("uri", configFileUri.toASCIIString());
                throw cle;
            }

            return client.getAsInputStream(configFileUri.getPath());
        }

        try {
            return configFileUri.toURL().openStream();
        } catch (IllegalArgumentException e) {
            String msg = "Can't open file as stream";
            ConfigurationLoadException cle = new ConfigurationLoadException(msg);
            cle.setContextValue("uri", configFileUri.toASCIIString());
            throw cle;
        }
    }
}
