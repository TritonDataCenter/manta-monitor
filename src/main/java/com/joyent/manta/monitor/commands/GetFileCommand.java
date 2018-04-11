/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.commands;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaObjectInputStream;
import com.joyent.manta.http.MantaHttpHeaders;
import com.joyent.manta.monitor.MantaOperationContext;
import org.bouncycastle.util.encoders.Hex;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

public class GetFileCommand implements MantaOperationCommand {
    public static final GetFileCommand INSTANCE = new GetFileCommand();

    @Override
    public boolean execute(final MantaOperationContext context) throws Exception {
        final MantaClient client = context.getMantaClient();
        final String filePath = context.getFilePath();
        final long expectedFileSize = requireNonNull(context.getTestFileSize());
        final MessageDigest checksum = MessageDigest.getInstance("SHA256");
        Long actualFileSize;

        try (MantaObjectInputStream in = client.getAsInputStream(filePath);
             DigestInputStream digestIn = new DigestInputStream(in, checksum)) {
            MantaHttpHeaders headers = in.getHttpHeaders();
            actualFileSize = headers.getContentLength();

            if (expectedFileSize != actualFileSize) {
                String msg = "The file size on Manta doesn't match the original size";
                DataValidationException dve =  new DataValidationException(msg);
                dve.setContextValue("path", context.getFilePath());
                dve.setContextValue("expectedFileSize", context.getTestFileSize());
                dve.setContextValue("actualFileSize", actualFileSize);
                throw dve;
            }

            final byte[] buf = new byte[8192];
            while (digestIn.read(buf) > -1);
        }

        final byte[] sha256 = checksum.digest();

        if (!Arrays.equals(sha256, context.getTestFileChecksum())) {
            String msg = "The checksum for the file uploaded and file downloaded "
                    + "do not match";
            DataValidationException dve =  new DataValidationException(msg);
            dve.setContextValue("expectedSha256sum", Hex.toHexString(context.getTestFileChecksum()));
            dve.setContextValue("actualSha256sum", Hex.toHexString(sha256));
            dve.setContextValue("path", context.getFilePath());
            dve.setContextValue("expectedFileSize", context.getTestFileSize());
            dve.setContextValue("actualFileSize", actualFileSize);
            throw dve;
        }

        return CONTINUE_PROCESSING;
    }
}
