/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.commands;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.monitor.MantaOperationContext;
import io.prometheus.client.Histogram;

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
        Histogram.Timer timer = context.getRequestPutHistogramsMap()
                .get(context.getChainClassNameKey()).startTimer();

        client.putDirectory(dir, true);

        timer.observeDuration();
        return CONTINUE_PROCESSING;
    }
}
