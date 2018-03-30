/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import com.joyent.manta.client.MantaClient;
import org.apache.commons.chain.Context;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MantaOperationContext extends ConcurrentHashMap implements Context {
    private static final long serialVersionUID = -81700974895811279L;

    public static final String MANTA_CLIENT_KEY = "mantaClient";
    public static final String MIN_FILE_SIZE_KEY = "minFileSize";
    public static final String MAX_FILE_SIZE_KEY = "maxFileSize";
    public static final String FILE_PATH_GEN_FUNC_KEY = "filePathGenerationFunction";
    public static final String FILE_PATH_KEY = "filePath";
    public static final String TEST_FILE_KEY = "testFile";
    public static final String TEST_FILE_CHECKSUM_KEY = "testFileChecksum";
    public static final String TEST_FILE_CHECKSUM_AS_STRING_KEY = "testFileChecksumString";
    public static final String RESPONSE_TIMES_KEY = "responseTime";
    public static final String EXCEPTION_KEY = "exception";

    public MantaOperationContext() {
        super();
        put(RESPONSE_TIMES_KEY, new ConcurrentHashMap<UUID, Integer>());
    }

    @Override
    public void clear() {
        super.clear();
        put(RESPONSE_TIMES_KEY, new ConcurrentHashMap<UUID, Integer>());
    }

    public MantaClient getMantaClient() {
        return (MantaClient)get(MANTA_CLIENT_KEY);
    }

    public MantaOperationContext setMantaClient(final MantaClient mantaClient) {
        put(MANTA_CLIENT_KEY, requireNonNull(mantaClient));
        return this;
    }

    public Integer getMinFileSize() {
        return (Integer)get(MIN_FILE_SIZE_KEY);
    }

    public MantaOperationContext setMinFileSize(final int minFileSize) {
        put(MIN_FILE_SIZE_KEY, minFileSize);
        return this;
    }

    public Integer getMaxFileSize() {
        return (Integer)get(MAX_FILE_SIZE_KEY);
    }

    public MantaOperationContext setMaxFileSize(final int maxFileSize) {
        put(MAX_FILE_SIZE_KEY, maxFileSize);
        return this;
    }

    public Function<byte[], String> getFilePathGenerationFunction() {
        return (Function<byte[], String>)get(FILE_PATH_GEN_FUNC_KEY);
    }

    public MantaOperationContext setFilePathGenerationFunction(final Function<byte[], String> function) {
        put(FILE_PATH_GEN_FUNC_KEY, requireNonNull(function));
        return this;
    }

    public String getFilePath() {
        return (String)get(FILE_PATH_KEY);
    }

    public MantaOperationContext setFilePath(final String path) {
        put(FILE_PATH_KEY, requireNonNull(path));
        return this;
    }

    public Path getTestFile() {
        return (Path)get(TEST_FILE_KEY);
    }

    public MantaOperationContext setTestFile(final Path testFile){
        put(TEST_FILE_KEY, requireNonNull(testFile));
        return this;
    }

    public byte[] getTestFileChecksum() {
        return (byte[])get(TEST_FILE_CHECKSUM_KEY);
    }

    public MantaOperationContext setTestFileChecksum(final byte[] checksum) {
        put(TEST_FILE_CHECKSUM_KEY, requireNonNull(checksum));
        put(TEST_FILE_CHECKSUM_AS_STRING_KEY, Hex.encodeHexString(checksum));
        return this;
    }

    public String getTestFileChecksumAsString() {
        return (String)get(TEST_FILE_CHECKSUM_AS_STRING_KEY);
    }

    public Map<UUID, Integer> getResponseTimes() {
        return (Map<UUID, Integer>)get(RESPONSE_TIMES_KEY);
    }

    @Nullable
    public Exception getException() {
        return (Exception)get(EXCEPTION_KEY);
    }

    public MantaOperationContext setException(final Exception exception) {
        put(EXCEPTION_KEY, exception);
        return this;
    }
}
