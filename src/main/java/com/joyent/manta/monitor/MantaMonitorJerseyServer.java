/*
 * Copyright (c) 2019, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.joyent.manta.client.MantaClient;
import io.logz.guice.jersey.GuiceJerseyResourceConfig;
import io.logz.guice.jersey.JettyServerCreator;
import io.logz.guice.jersey.configuration.JerseyConfiguration;
import io.logz.guice.jersey.configuration.ServerConnectorConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.DispatcherType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Customized version of the {@link io.logz.guice.jersey.JerseyServer}.
 * This class adds configuration of SSL connector which handling HTTPS.
 */

public class MantaMonitorJerseyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MantaMonitorJerseyServer.class);
    private final JerseyConfiguration jerseyConfiguration;
    private final GuiceServletContextListener contextListener;
    private final Server server;
    private final MantaClient client;

    @Inject
    MantaMonitorJerseyServer(final JerseyConfiguration jerseyConfiguration,
                             final JettyServerCreator jettyServerCreator,
                             final GuiceServletContextListener contextListener,
                             final MantaClient client) {
        this.jerseyConfiguration = jerseyConfiguration;
        this.server = jettyServerCreator.create();
        this.contextListener = contextListener;
        this.client = client;
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
            MantaMonitorJerseyServerException exception = new MantaMonitorJerseyServerException(message.toString());
            throw exception;
        }
    }

    private int validateSecurePort(final String securePortEnvVariable) {
        if (securePortEnvVariable == null) {
            String message = "No null value allowed for JETTY_SERVER_SECURE_PORT when ENABLE_TLS is set to true";
            MantaMonitorJerseyServerException exception = new MantaMonitorJerseyServerException(message);
            throw exception;
        }
        if (Integer.parseInt(securePortEnvVariable) <= 0) {
            String message = "Jetty server secure port must be greater than 0";
            MantaMonitorJerseyServerException exception = new MantaMonitorJerseyServerException(message);
            exception.setContextValue("JETTY_SERVER_SECURE_PORT", securePortEnvVariable);
            throw exception;
        } else {
            return Integer.parseInt(securePortEnvVariable);
        }
    }

    private URI getURIFromString(final String uriString) {
        URI configUri = null;

        try {
            configUri = URI.create(uriString);

            if (configUri.getScheme() == null) {
                File file = new File(uriString);

                if (!file.exists()) {
                    LOGGER.error("File does not exist: {}", uriString);
                    System.exit(1);
                }

                try {
                    configUri = file.getCanonicalFile().toURI();
                } catch (IOException ioe) {
                    LOGGER.error("Unable to convert file to URI", ioe);
                    System.exit(1);
                }
            }

        } catch (IllegalArgumentException | NullPointerException e) {
            String msg = String.format("Invalid URI for store path: %s",
                    uriString);
            LOGGER.error(msg, e);
            System.exit(1);
        }

        return configUri;
    }

    private InputStream readFileFromURI(final URI uriFile) throws IOException {
        if ("manta".equals(uriFile.getScheme())) {
            if (client == null) {
                String msg = "Can't load file from Manta when MantaClient is null";
                MantaMonitorJerseyServerException exception = new MantaMonitorJerseyServerException(msg);
                exception.setContextValue("uri", uriFile.toASCIIString());
                throw exception;
            }
            return client.getAsInputStream(uriFile.getPath());
        }

        try {
            return uriFile.toURL().openStream();
        } catch (IllegalArgumentException e) {
            String msg = "Can't open file as stream";
            MantaMonitorJerseyServerException exception = new MantaMonitorJerseyServerException(msg);
            exception.setContextValue("uri", uriFile.toASCIIString());
            throw exception;
        }
    }

    private void writeInputStreamToTarget(final URI srcURI,
                                          final File targetFile) {
        try {
            InputStream in = readFileFromURI(srcURI);
            FileUtils.copyInputStreamToFile(in, targetFile);
        } catch (IOException ie) {
            String msg = "Failed to write stream to target file. "
                    + "Check if the file exists on the URI path";
            MantaMonitorJerseyServerException exception = new MantaMonitorJerseyServerException(msg);
            exception.setContextValue("uriString", srcURI.toASCIIString());
            exception.setContextValue("targetFilePath", targetFile.getPath());
            throw exception;
        }

    }

    /**
     * This method configures the embedded jetty server to run on either the http
     * mode on JETTY_SERVER_PORT or the https mode on JETTY_SERVER_SECURE_PORT.
     * If the ENABLE_TLS flag is set to true during runtime, the server will
     * operate only in the https mode.
     */
    private void configureServer() {
        if (BooleanUtils.toBoolean(System.getenv("ENABLE_TLS"))) {
            // Activate only the JETTY_SERVER_SECURE_PORT since ENABLE_TLS is
            // set to true
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

            String userHomePath = System.getProperty("user.home");

            URI keystoreURI = getURIFromString(System.getenv("KEYSTORE_PATH"));

            if ("manta".equals(keystoreURI.getScheme())) {
                // Make the keystore file available for the sslContextFactory, by
                // reading the remote file and writing it to a file on the host file system.
                File keystoreTargetFile = new File(userHomePath + "/keystore");
                writeInputStreamToTarget(keystoreURI, keystoreTargetFile);
                sslContextFactory.setKeyStorePath(keystoreTargetFile.getPath());
            } else {
                // Read the keystore directly from the local file system
                sslContextFactory.setKeyStorePath(System.getenv("KEYSTORE_PATH"));
            }

            URI trustStoreURI = getURIFromString(System.getenv("TRUSTSTORE_PATH"));
            if ("manta".equals(trustStoreURI.getScheme())) {
                // Make the truststore file available for the sslContextFactory, by
                // reading the remote file and writing it to a file on the host file system.
                File trustStoreTargetFile = new File(userHomePath + "/truststore");
                writeInputStreamToTarget(trustStoreURI, trustStoreTargetFile);
                sslContextFactory.setTrustStorePath(trustStoreTargetFile.getPath());
            } else {
                // Read the truststore directly from the local file system
                sslContextFactory.setTrustStorePath(System.getenv("TRUSTSTORE_PATH"));
            }
            sslContextFactory.setKeyStorePassword(System.getenv("KEYSTORE_PASS"));
            sslContextFactory.setKeyManagerPassword(System.getenv("KEYSTORE_PASS"));
            sslContextFactory.setTrustStorePassword(System.getenv("TRUSTSTORE_PASS"));
            sslContextFactory.setNeedClientAuth(true);

            ServerConnector httpsConnector = new ServerConnector(this.server,
                    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfig));
            httpsConnector.setPort(securePort);

            this.server.addConnector(httpsConnector);
        } else {
            // Activate only the JETTY_SERVER_PORT since ENABLE_TLS is
            // either set to false or is null
            List<ServerConnectorConfiguration> serverConnectorConfigurations = this.jerseyConfiguration.getServerConnectors();
            serverConnectorConfigurations.forEach((configuration) -> {
                ServerConnector connector = new ServerConnector(this.server);
                connector.setName(configuration.getName());
                connector.setHost(configuration.getHost());
                connector.setPort(configuration.getPort());
                this.server.addConnector(connector);
            });
        }
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setServer(this.server);
        webAppContext.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
        ServletHolder holder = new ServletHolder(ServletContainer.class);
        holder.setInitParameter("javax.ws.rs.Application", GuiceJerseyResourceConfig.class.getName());
        webAppContext.addServlet(holder, "/*");
        webAppContext.setResourceBase("/");
        webAppContext.setContextPath(this.jerseyConfiguration.getContextPath());
        webAppContext.addEventListener(this.contextListener);
        this.server.setHandler(webAppContext);
    }
}
