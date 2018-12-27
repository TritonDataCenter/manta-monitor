/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.commands;

import com.joyent.manta.monitor.MantaOperationContext;
import com.joyent.manta.monitor.RandomAlphabeticInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

/**
 * This {@link org.apache.commons.chain.Command} implementation generates the test file.
 */
public class GenerateFileCommand implements MantaOperationCommand {
    public static final GenerateFileCommand INSTANCE = new GenerateFileCommand();

    @Override
    public boolean execute(final MantaOperationContext context) throws Exception {
        context.getStopWatch().start();
        final long filesize = generateFileSize(context);
        context.setTestFileSize(filesize);

        final Path temp = Files.createTempFile(String.format("mput-%s-",
                LocalDate.now().format(ISO_LOCAL_DATE)),
                ".txt");

        /* Register the file for deletion upon the JVM's exit, so that
         * we don't have junk cluttering up our filesystem. */
        FileUtils.forceDeleteOnExit(temp.toFile());

        final MessageDigest checksum = MessageDigest.getInstance("SHA256");

        try (RandomAlphabeticInputStream in = new RandomAlphabeticInputStream(filesize);
             DigestInputStream digestIn = new DigestInputStream(in, checksum);
             OutputStream out = Files.newOutputStream(temp)) {
            IOUtils.copy(digestIn, out);
        }

        final byte[] sha256 = checksum.digest();

        context.setTestFile(temp)
               .setTestFileChecksum(sha256);

        return CONTINUE_PROCESSING;
    }

    private static long generateFileSize(final MantaOperationContext context) {
        final int min = context.getMinFileSize();
        final int max = context.getMaxFileSize();

        return RandomUtils.nextInt(min, max);
    }
}
