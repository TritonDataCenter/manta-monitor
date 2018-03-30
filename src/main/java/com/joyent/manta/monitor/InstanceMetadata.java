/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Class containing metadata about the running instance used for classifying
 * errors in Honeybadger.
 */
public class InstanceMetadata {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceMetadata.class);
    private static final String PROPS_FILE_PATH_ENV_VAR = "INSTANCE_METADATA_PROPS_FILE";

    private String id;
    private String imageId;
    private String serverId;
    private String ownerId;
    private String alias;
    private String datacenterName;
    private String piVersion;

    private final boolean isOnLxZone;
    private final String basePath;
    private final Properties properties;

    public InstanceMetadata() {
        this(new Properties());
    }

    public InstanceMetadata(final Properties properties) {
        this.properties = properties;
        this.isOnLxZone = isOnLxZone();

        if (isOnLxZone) {
            this.basePath = "/native";
        } else {
            this.basePath = "";
        }

        loadProperties();
        populateMetadata();
    }

    private void loadProperties() {
        final String metadataPropsFile = System.getenv(PROPS_FILE_PATH_ENV_VAR);

        if (StringUtils.isBlank(metadataPropsFile)) {
            return;
        }

        final Path propsFilePath = Paths.get(metadataPropsFile);
        if (Files.notExists(propsFilePath)) {
            return;
        }

        try (InputStream in = Files.newInputStream(propsFilePath)) {
            this.properties.load(in);
        } catch (IOException e) {
            String msg = String.format("Can't read instance metadata properties from: %s",
                    metadataPropsFile);
            LOG.warn(msg, e);
        }
    }

    public String getId() {
        return id;
    }

    public InstanceMetadata setId(final String id) {
        this.id = id;
        return this;
    }

    public String getImageId() {
        return imageId;
    }

    public InstanceMetadata setImageId(final String imageId) {
        this.imageId = imageId;
        return this;
    }

    public String getServerId() {
        return serverId;
    }

    public InstanceMetadata setServerId(final String serverId) {
        this.serverId = serverId;
        return this;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public InstanceMetadata setOwnerId(final String ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public String getAlias() {
        return alias;
    }

    public InstanceMetadata setAlias(final String alias) {
        this.alias = alias;
        return this;
    }

    public String getDatacenterName() {
        return datacenterName;
    }

    public InstanceMetadata setDatacenterName(final String datacenterName) {
        this.datacenterName = datacenterName;
        return this;
    }

    public String getPiVersion() {
        return piVersion;
    }

    public InstanceMetadata setPiVersion(final String piVersion) {
        this.piVersion = piVersion;
        return this;
    }

    public Set<String> asTagSet() {
        ImmutableSet.Builder<String> tags = new ImmutableSet.Builder<>();

        if (isNotBlank(datacenterName)) {
            tags.add(datacenterName);
        }

        if (isNotBlank(piVersion)) {
            tags.add(piVersion);
        }

        return tags.build();
    }

    public Map<String, String> asMap() {
        return constructMap(
                "Instance ID", id,
                "Platform Image", piVersion,
                "Image ID", imageId,
                "Server ID", serverId,
                "Owner ID", ownerId,
                "Alias", alias,
                "Datacenter Name", datacenterName);
    }

    private Map<String, String> constructMap(final String... kv) {
        if (kv == null || kv.length == 0) {
            return Collections.emptyMap();
        }

        final ImmutableMap.Builder<String, String> map = new ImmutableMap.Builder<>();

        try {
            String key = null;
            for (int i = 0; i < kv.length; i++) {
                final String keyOrVal = kv[i];
                final boolean isKey = i % 2 == 0 || i == 0;

                if (isBlank(keyOrVal)) {
                    if (isKey) {
                        i++;
                    }
                    continue;
                }

                if (isKey) {
                    key = keyOrVal;
                } else {
                    map.put(key, keyOrVal);
                }
            }
        } catch (NullPointerException e) {
            String msg = String.format("Input parameters malformed. Input params: %s",
                    ReflectionToStringBuilder.toString(kv));
            throw new IllegalArgumentException(msg, e);
        }

        return map.build();
    }

    private static boolean isOnLxZone() {
        if (!SystemUtils.IS_OS_LINUX) {
            return false;
        }

        final String unameV = runSimpleCommand(new String[] {"uname", "-v"});
        return "BrandZ virtual linux".equals(unameV);
    }

    private void populateMetadata() {
        if (isOnLxZone || SystemUtils.IS_OS_SOLARIS) {
            setPiVersion(properties.getProperty("pi_version",
                    readUnameV()));
        } else {
            setPiVersion(properties.getProperty("pi_version"));
        }

        setId(readProp("sdc:uuid"));
        setImageId(readProp("sdc:image_uuid"));
        setAlias(readProp("sdc:alias"));
        setDatacenterName(readProp("sdc:datacenter_name"));
        setOwnerId(readProp("sdc:owner_uuid"));
        setServerId(readProp("sdc:server_uuid"));
    }

    private String readUnameV() {
        return runSimpleCommand(new String[] {basePath + "/usr/bin/uname", "-v"});
    }

    private String readProp(final String param) {
        final String sysPropName = StringUtils.substringAfter(param, "sdc:");

        if (isOnLxZone || SystemUtils.IS_OS_SOLARIS) {
            return properties.getProperty(sysPropName, mdataGet(param));
        } else {
            return properties.getProperty(sysPropName);
        }
    }

    private String mdataGet(final String param) {
        return runSimpleCommand(new String[] {basePath + "/usr/sbin/mdata-get", param});
    }

    private static String runSimpleCommand(final String[] cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));

            StringBuilder lines = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                lines.append(line);
                line = br.readLine();
                if (line != null) {
                    lines.append(System.lineSeparator());
                }
            }

            return lines.toString();
        } catch (IOException e) {
            String msg = String.format("Error running: %s",
                    ReflectionToStringBuilder.toString(cmd));
            LOG.warn(msg, e);
            return "";
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final InstanceMetadata that = (InstanceMetadata) o;

        return Objects.equals(id, that.id)
                && Objects.equals(imageId, that.imageId)
                && Objects.equals(serverId, that.serverId)
                && Objects.equals(ownerId, that.ownerId)
                && Objects.equals(alias, that.alias)
                && Objects.equals(datacenterName, that.datacenterName)
                && Objects.equals(piVersion, that.piVersion);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, imageId, serverId,
                ownerId, alias, datacenterName,
                piVersion);
    }

    public static void main(final String[] argv) {
        System.out.println("Dumping instance metadata");
        InstanceMetadata metadata = new InstanceMetadata();

        System.out.println();

        for (Map.Entry<String, String> entry : metadata.asMap().entrySet()) {
            String key = StringUtils.rightPad(entry.getKey() + ":", 17);
            System.out.print(key);
            System.out.println(entry.getValue());
        }
    }
}
