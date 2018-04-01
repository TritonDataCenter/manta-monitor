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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

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

        for (int i = dirs.length - 1; i >= 0; i--) {
            StringBuilder dir = new StringBuilder(baseDir);
            for (int j = 0; j <= i; j++) {
                dir.append(SEPARATOR).append(dirs[j]);
            }

            client.delete(dir.toString());
        }

        if (client.isDirectoryEmpty(baseDir)) {
            client.delete(baseDir);
        } else {
            client.deleteRecursive(baseDir);
        }

        return PROCESSING_COMPLETE;
    }
}
