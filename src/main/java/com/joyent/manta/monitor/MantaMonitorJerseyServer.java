/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import io.logz.guice.jersey.GuiceJerseyResourceConfig;
import io.logz.guice.jersey.JettyServerCreator;
import io.logz.guice.jersey.configuration.JerseyConfiguration;
import io.logz.guice.jersey.configuration.ServerConnectorConfiguration;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * Customized version of the {@link io.logz.guice.jersey.JerseyServer}.
 * This class adds configuration of SSL connector which handling HTTPS.
 */

public class MantaMonitorJerseyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MantaMonitorJerseyServer.class);
    private final JerseyConfiguration jerseyConfiguration;
    private final Supplier<Injector> injectorSupplier;
    private final Server server;

    MantaMonitorJerseyServer(final JerseyConfiguration jerseyConfiguration,
                             final Supplier<Injector> injectorSupplier,
                             final JettyServerCreator jettyServerCreator) {
        this.jerseyConfiguration = jerseyConfiguration;
        this.injectorSupplier = injectorSupplier;
        this.server = jettyServerCreator.create();
        this.configureServer();
    }

    public void start() throws Exception {
        LOGGER.info("Starting embedded jetty server");
        this.server.start();
    }

    public void stop() throws Exception {
        this.server.stop();
        LOGGER.info("Embedded jetty server stopped");
    }

    private void validateKeyStoreEnvVariables() {
        String serverKeyStorePath = System.getenv("KEYSTORE_PATH");
        String serverKeyStorePass = System.getenv("KEYSTORE_PASS");
        String trustedKeyStorePath = System.getenv("TRUSTSTORE_PATH");
        String trustedKeyStorePass = System.getenv("TRUSTSTORE_PASS");
        if (serverKeyStorePath == null || serverKeyStorePass == null
                || trustedKeyStorePath == null || trustedKeyStorePass == null) {
            ArrayList<String> nullEnvVariablesList = new ArrayList<>();
            if (serverKeyStorePath == null) {
                nullEnvVariablesList.add("KEYSTORE_PATH");
            }
            if (serverKeyStorePass == null) {
                nullEnvVariablesList.add("KEYSTORE_PASS");
            }
            if (trustedKeyStorePath == null) {
                nullEnvVariablesList.add("TRUSTSTORE_PATH");
            }
            if (trustedKeyStorePass == null) {
                nullEnvVariablesList.add("TRUSTSTORE_PASS");
            }
            reportNullKeyStoreVariables(nullEnvVariablesList);
        }
    }

    private void reportNullKeyStoreVariables(final ArrayList<String> nullKeyStoreEnvVariablesList) {
        if (!nullKeyStoreEnvVariablesList.isEmpty()) {
            StringBuffer message = new StringBuffer("Null values encountered for env variables:");
            message.append(System.lineSeparator());
            for (String envVariable : nullKeyStoreEnvVariablesList) {
                message.append(envVariable);
                message.append(System.lineSeparator());
            }
            throw new RuntimeException(message.toString());
        }
    }

    private int validateSecurePort(final String securePortEnvVariable) {
        if (securePortEnvVariable == null) {
            throw new RuntimeException("To enable TLS env variable JETTY_SERVER_SECURE_PORT is required");
        }
        if (Integer.parseInt(securePortEnvVariable) <= 0) {
            throw new RuntimeException("Jetty server secure port must be greater than 0");
        } else {
            return Integer.parseInt(securePortEnvVariable);
        }
    }

    private void configureServer() {
        List<ServerConnectorConfiguration> serverConnectorConfigurations = this.jerseyConfiguration.getServerConnectors();
        serverConnectorConfigurations.forEach((configuration) -> {
            ServerConnector connector = new ServerConnector(this.server);
            connector.setName(configuration.getName());
            connector.setHost(configuration.getHost());
            connector.setPort(configuration.getPort());
            this.server.addConnector(connector);
        });

        if (System.getenv("ENABLE_TLS") != null && System.getenv("ENABLE_TLS").equals("true")) {
            validateKeyStoreEnvVariables();
            int securePort = validateSecurePort(System.getenv("JETTY_SERVER_SECURE_PORT"));
            HttpConfiguration httpConfig = new HttpConfiguration();
            httpConfig.setSecureScheme("https");
            httpConfig.setSecurePort(securePort);

            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            SecureRequestCustomizer src = new SecureRequestCustomizer();
            src.setStsMaxAge(2000);
            src.setStsIncludeSubDomains(true);
            httpsConfig.addCustomizer(src);

            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(System.getenv("KEYSTORE_PATH"));
            sslContextFactory.setKeyStorePassword(System.getenv("KEYSTORE_PASS"));
            sslContextFactory.setKeyManagerPassword(System.getenv("KEYSTORE_PASS"));
            sslContextFactory.setTrustStorePath(System.getenv("TRUSTSTORE_PATH"));
            sslContextFactory.setTrustStorePassword(System.getenv("TRUSTSTORE_PASS"));
            sslContextFactory.setNeedClientAuth(true);

            ServerConnector httpsConnector = new ServerConnector(this.server,
                    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfig));
            httpsConnector.setPort(securePort);

            this.server.addConnector(httpsConnector);
        }
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setServer(this.server);
        webAppContext.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
        ServletHolder holder = new ServletHolder(ServletContainer.class);
        holder.setInitParameter("javax.ws.rs.Application", GuiceJerseyResourceConfig.class.getName());
        webAppContext.addServlet(holder, "/*");
        webAppContext.setResourceBase("/");
        webAppContext.setContextPath(this.jerseyConfiguration.getContextPath());
        webAppContext.addEventListener(new GuiceServletContextListener() {
            protected Injector getInjector() {
                return MantaMonitorJerseyServer.this.injectorSupplier.get();
            }
        });
        this.server.setHandler(webAppContext);
    }
}
