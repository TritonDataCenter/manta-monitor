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

import java.io.IOException;

/**
 * {@link org.apache.commons.chain.Command} implementation that deletes all of
 * the directories (and any files left over) within the base directory path.
 */
public class CleanupCommand implements MantaOperationCommand {
    public static final CleanupCommand INSTANCE = new CleanupCommand();

    @Override
    public boolean execute(final MantaOperationContext context) throws Exception {
        final MantaClient client = context.getMantaClient();

        deleteObject(client, context.getFilePath());

        context.getStopWatch().stop();
        return PROCESSING_COMPLETE;
    }

    private static void deleteObject(final MantaClient client,
                                     final String object) throws IOException {
        try {
            /* We are relying on the prune directory settings to delete a certain
             * number of subdirectories under the object. */
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
