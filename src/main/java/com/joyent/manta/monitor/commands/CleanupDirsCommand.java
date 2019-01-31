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
import com.joyent.manta.monitor.MantaOperationContext;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

import static com.joyent.manta.client.MantaClient.SEPARATOR;

/**
 * {@link org.apache.commons.chain.Command} implementation that deletes all of
 * the directories (and any files left over) within the base directory path.
 */
public class CleanupDirsCommand implements MantaOperationCommand {
    public static final CleanupDirsCommand INSTANCE = new CleanupDirsCommand();

    @Override
    public boolean execute(final MantaOperationContext context) throws Exception {
        final MantaClient client = context.getMantaClient();
        final String baseDir = context.getTestBaseDir();
        final String lowestDir = FilenameUtils.getFullPathNoEndSeparator(context.getFilePath());
        final String lowestDirWithoutBase = StringUtils.substringAfter(lowestDir, baseDir);
        final String[] dirs = StringUtils.split(lowestDirWithoutBase, SEPARATOR);

        /* We want to delete only the files and directories related to the
         * directory structure in the file sent in this chain and no others. */
        for (int i = dirs.length - 1; i >= 0; i--) {
            StringBuilder dir = new StringBuilder(baseDir);
            for (int j = 0; j <= i; j++) {
                dir.append(SEPARATOR).append(dirs[j]);
            }

            deleteObject(client, dir.toString());
        }

        deleteObject(client, baseDir);

        context.getStopWatch().stop();
        return PROCESSING_COMPLETE;
    }

    private static void deleteObject(final MantaClient client,
                                     final String object) throws IOException {
        try {
            client.delete(object);
        } catch (MantaClientHttpResponseException e) {
            switch (e.getServerCode()) {
                case RESOURCE_NOT_FOUND_ERROR:
                    // do nothing because the object is already gone
                case DIRECTORY_NOT_EMPTY_ERROR:
                    // do nothing because the directory contains other objects
                    break;
                default:
                    throw e;
            }
        }
    }
}
