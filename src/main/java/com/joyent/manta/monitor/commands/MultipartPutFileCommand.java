/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.commands;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaMetadata;
import com.joyent.manta.client.multipart.MantaMultipartUploadPart;
import com.joyent.manta.client.multipart.ServerSideMultipartManager;
import com.joyent.manta.client.multipart.ServerSideMultipartUpload;
import com.joyent.manta.http.MantaHttpHeaders;
import com.joyent.manta.monitor.MantaOperationContext;
import com.joyent.manta.monitor.MantaOperationException;
import io.prometheus.client.Histogram;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.CloseShieldInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * {@link org.apache.commons.chain.Command} implementation that uploads a file
 * to Manta using MPU.
 */
public class MultipartPutFileCommand extends PutFileCommand {
    public static final MultipartPutFileCommand INSTANCE = new MultipartPutFileCommand();

    private static final int MAX_PARTS = 10;

    @Override
    public boolean execute(final MantaOperationContext context) throws Exception {
        final MantaClient client = context.getMantaClient();
        final ServerSideMultipartManager multipartManager = new ServerSideMultipartManager(client);

        final String filePath = generateFilePath(context);
        context.setFilePath(filePath);

        try {
            Histogram.Timer timer = context.getRequestPutHistogramsKey()
                    .get(context.getChainClassNameKey()).startTimer();
            final ServerSideMultipartUpload upload = buildMultipartUpload(
                    filePath, context, multipartManager);
            final int totalParts = calculateNumberOfParts(context,
                    multipartManager.getMinimumPartSize());
            Set<MantaMultipartUploadPart> parts = createAndUploadParts(upload, context, totalParts, multipartManager);
            multipartManager.complete(upload, parts);
            timer.observeDuration();
        } finally {
            Files.deleteIfExists(context.getTestFile());
        }

        return CONTINUE_PROCESSING;
    }

    private static long validateTestFileSize(final MantaOperationContext context,
                                             final ServerSideMultipartManager multipartManager)
            throws IOException {
        final long fileSize = validateTestFileSize(context);

        if (fileSize < multipartManager.getMinimumPartSize()) {
            String msg = String.format("File size too small for multipart upload"
                            + " [minimum_size=%d,actual_size=%d]",
                    multipartManager.getMinimumPartSize(), fileSize);
            throw new IllegalArgumentException(msg);
        }

        return fileSize;
    }

    private static ServerSideMultipartUpload buildMultipartUpload(
            final String filePath,
            final MantaOperationContext context,
            final ServerSideMultipartManager multipartManager) throws IOException {
        final long fileSize = validateTestFileSize(context, multipartManager);
        final MantaHttpHeaders headers = buildHeaders();
        final MantaMetadata metadata = buildMetadata(context);
        return multipartManager.initiateUpload(
                filePath, fileSize, metadata, headers);
    }

    private ImmutableSet<MantaMultipartUploadPart> createAndUploadParts(
            final ServerSideMultipartUpload upload,
            final MantaOperationContext context,
            final int totalParts,
            final ServerSideMultipartManager multipartManager) throws IOException {
        final Path testFilePath = context.getTestFile();
        final long fileSize = context.getTestFileSize();
        final int minimumPartSize = multipartManager.getMinimumPartSize();

        final long[] chunkSizes = calculateChunkSizes(fileSize, totalParts,
                minimumPartSize);

        final ImmutableSet.Builder<MantaMultipartUploadPart> parts = new ImmutableSet.Builder<>();

        try (InputStream pathIn = Files.newInputStream(testFilePath)) {
            for (int i = 0; i < chunkSizes.length; i++) {
                final int partNumber = i + 1;
                final long chunkSize = chunkSizes[i];

                BoundedInputStream bounded = new BoundedInputStream(pathIn, chunkSize);
                CloseShieldInputStream cin = new CloseShieldInputStream(bounded);

                MantaMultipartUploadPart part = multipartManager.uploadPart(upload, partNumber, cin);
                parts.add(part);
            }
        } catch (RuntimeException e) {
            try {
                multipartManager.abort(upload);
            } catch (RuntimeException aboutException) {
                LOG.error("Error aborting MPU after failed part upload", e);
            }

            MantaOperationException moe = new MantaOperationException(e);
            moe.setContextValue("path", upload.getPath());

            throw moe;
        } finally {
            Files.deleteIfExists(testFilePath);
        }

        return parts.build();
    }

    private static int calculateNumberOfParts(final MantaOperationContext context,
                                              final int minimumPartSize) {
        final long fileSize = context.getTestFileSize();

        if (fileSize < minimumPartSize) {
            String msg = String.format("Unable to calculate file parts count "
                    + "because the file size [%d] is less than the minimum "
                    + "part size [%d]", fileSize, minimumPartSize);
            throw new IllegalArgumentException(msg);
        }

        final long totalPossibleParts = fileSize / minimumPartSize;

        if (totalPossibleParts > 10) {
            return MAX_PARTS;
        } else {
            return (int)totalPossibleParts;
        }
    }

    @VisibleForTesting
    static long[] calculateChunkSizes(final long fileSize, final int totalParts,
                                      final int minimumPartSize) {
        final long chunkSize = fileSize / totalParts;
        final long remainder = fileSize - (chunkSize * totalParts);

        final long remainderPerPart = remainder / totalParts;
        final long remainderOfRemainder = remainder - (remainderPerPart * totalParts);

        final long[] chunkSizes = new long[totalParts];

        for (int i = 0; i < totalParts; i++) {
            chunkSizes[i] = chunkSize + remainderPerPart;

            if (i == 0) {
                chunkSizes[i] += remainderOfRemainder;
            }

            if (chunkSizes[i] < minimumPartSize) {
                String msg = String.format("Unable to split file into parts "
                    + "that conform to the minimum part size [%d] for the "
                    + "file size [%d] with [%d] parts", minimumPartSize,
                        fileSize, totalParts);
                throw new IllegalArgumentException(msg);
            }
        }

        return chunkSizes;
    }
}
