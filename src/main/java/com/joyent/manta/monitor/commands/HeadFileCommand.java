/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.commands;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaObjectResponse;
import com.joyent.manta.monitor.MantaOperationContext;

import java.io.IOException;
import java.util.Objects;

/**
 * This {@link org.apache.commons.chain.Command} implementation checks to see
 * if our test file exists and its metadata matches our expectations.
 */
public class HeadFileCommand implements MantaOperationCommand {
    public static final HeadFileCommand INSTANCE = new HeadFileCommand();

    @Override
    public boolean execute(final MantaOperationContext context) throws IOException {
        final MantaClient client = context.getMantaClient();
        final String path = context.getFilePath();
        final long fileSize = Objects.requireNonNull(context.getTestFileSize());
        final MantaObjectResponse response = client.head(path);
        final Long actualFileSize = response.getContentLength();

        if (actualFileSize != fileSize) {
            String msg = String.format("File written to filesystem [%d bytes] "
                            + "is not the same size as stored in context [%d bytes]",
                    actualFileSize, fileSize);
            DataValidationException dve =  new DataValidationException(msg);
            dve.setContextValue("path", context.getFilePath());
            dve.setContextValue("expectedFileSize", context.getTestFileSize());
            dve.setContextValue("actualFileSize", actualFileSize);
            throw dve;
        }

        final String actualChecksum = response.getMetadata().get("m-sha256-checksum");
        final String checksum = context.getTestFileChecksumAsString();

        if (!checksum.equals(actualChecksum)) {
            String msg = "The checksum for the file uploaded and file downloaded "
                    + "do not match";
            DataValidationException dve =  new DataValidationException(msg);
            dve.setContextValue("expectedSha256sum", checksum);
            dve.setContextValue("actualSha256sum", actualChecksum);
            dve.setContextValue("path", context.getFilePath());
            dve.setContextValue("expectedFileSize", context.getTestFileSize());
            dve.setContextValue("actualFileSize", actualFileSize);
            throw dve;
        }

        return CONTINUE_PROCESSING;
    }
}
