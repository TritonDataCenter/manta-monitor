/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.commands;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaMetadata;
import com.joyent.manta.client.MantaObjectResponse;
import com.joyent.manta.http.MantaHttpHeaders;
import com.joyent.manta.monitor.MantaOperationContext;
import com.joyent.manta.monitor.MantaOperationException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.UUID;

public class PutFileCommand implements MantaOperationCommand {
    public static final PutFileCommand INSTANCE = new PutFileCommand();

    @Override
    public boolean execute(final MantaOperationContext context) throws Exception {
        validateTestFileSize(context);

        final MantaClient client = context.getMantaClient();
        final String filePath = generateFilePath(context);
        context.setFilePath(filePath);

        final File file = context.getTestFile().toFile();

        try {
            final MantaHttpHeaders headers = buildHeaders();
            final MantaMetadata metadata = buildMetadata(context);

            /* Record the latencies per PUT operation so that we can act upon
             * pathological latency numbers. */
            final MantaObjectResponse response = client.put(
                    filePath, file, headers, metadata);
            final UUID requestId = UUID.fromString(response.getRequestId());
            final Integer responseTime = parseResponseTime(response.getHttpHeaders());
            context.getResponseTimes().put(requestId, responseTime);
        } catch (RuntimeException e) {
            throw new MantaOperationException(e).setPath(filePath);
        } finally {
            Files.deleteIfExists(context.getTestFile());
        }

        return CONTINUE_PROCESSING;
    }

    protected static long validateTestFileSize(final MantaOperationContext context)
            throws IOException {
        Objects.requireNonNull(context.getTestFile());
        final long fileSize = context.getTestFileSize();
        final long actualFileSize = Files.size(context.getTestFile());

        if (fileSize != actualFileSize) {
            String msg = String.format("File written to filesystem [%d bytes] "
                            + "is not the same size as stored in context [%d bytes]",
                    actualFileSize, fileSize);
            throw new IllegalArgumentException(msg);
        }

        return fileSize;
    }

    protected static MantaHttpHeaders buildHeaders() {
        final MantaHttpHeaders headers = new MantaHttpHeaders();
        headers.setContentType("text/plain; charset=UTF-8");
        return headers;
    }

    protected static MantaMetadata buildMetadata(final MantaOperationContext context) {
        final MantaMetadata metadata = new MantaMetadata();
        metadata.put("m-sha256-checksum", context.getTestFileChecksumAsString());
        return metadata;
    }

    protected static String generateFilePath(final  MantaOperationContext context) {
        final byte[] checksum = context.getTestFileChecksum();
        final String dir = context.getFilePathGenerationFunction().apply(checksum);

        return dir + context.getTestFileChecksumAsString() + ".txt";
    }

    protected static Integer parseResponseTime(final MantaHttpHeaders headers) {
        final String responseTimeAsString = Objects.toString(headers.get("x-response-time"));

        if (StringUtils.isBlank(responseTimeAsString)) {
            return null;
        }

        return Integer.parseInt(responseTimeAsString);
    }
}
