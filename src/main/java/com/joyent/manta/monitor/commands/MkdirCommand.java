/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.commands;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.exception.MantaClientHttpResponseException;
import com.joyent.manta.exception.MantaErrorCode;
import com.joyent.manta.monitor.MantaOperationContext;
import io.prometheus.client.Histogram;

import java.util.UUID;

/**
 * This {@link org.apache.commons.chain.Command} implementation creates a dir to
 * perform the cli command mput.
 */
public class MkdirCommand implements MantaOperationCommand {
    public static final MkdirCommand INSTANCE = new MkdirCommand();

    @Override
    public boolean execute(final MantaOperationContext context) throws Exception {
        final byte[] checksum = context.getTestFileChecksum();
        final String dir = context.getFilePathGenerationFunction().apply(checksum);
        final MantaClient client = context.getMantaClient();
         /**
          * Using the requestPutHistogramsMap from the context will ensure that
          * the same metric (i.e. manta_monitor_put_request_latency_seconds_FileUploadGetDeleteChain
          * is updated with cumulative values added here and in the
          * {@link PutFileCommand} and {@link MultipartPutFileCommand}
          */
        Histogram.Timer timer = context.getRequestPutHistograms()
                .get(context.getChainClassNameKey()).startTimer();

        if ("buckets".equals(context.getTestType())) {
            String bucketPath = String.format("%s-%s", context.getTestBaseDirOrBucket(),
                    UUID.randomUUID());
            context.setBucketPath(bucketPath);
            try {
                client.createBucket(bucketPath);
            } catch (MantaClientHttpResponseException e) {
                if (!e.getServerCode().equals(MantaErrorCode.BUCKET_EXISTS_ERROR)) {
                    throw e;
                }
            }

        } else {
            client.putDirectory(dir, true);
        }

        timer.observeDuration();
        return CONTINUE_PROCESSING;
    }
}
