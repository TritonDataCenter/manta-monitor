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

public class DeleteFileCommand implements MantaOperationCommand {
    public static final DeleteFileCommand INSTANCE = new DeleteFileCommand();

    @Override
    public boolean execute(final MantaOperationContext context) throws Exception {
        final MantaClient client = context.getMantaClient();
        final String filePath = context.getFilePath();

        client.delete(filePath);

        return PROCESSING_COMPLETE;
    }
}
