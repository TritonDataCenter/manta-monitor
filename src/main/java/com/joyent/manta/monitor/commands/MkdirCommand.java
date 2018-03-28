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

public class MkdirCommand implements MantaOperationCommand {
    public static final MkdirCommand INSTANCE = new MkdirCommand();

    @Override
    public boolean execute(final MantaOperationContext context) throws Exception {
        final byte[] checksum = context.getTestFileChecksum();
        final String dir = context.getFilePathGenerationFunction().apply(checksum);
        final MantaClient client = context.getMantaClient();

        client.putDirectory(dir, true);

        return CONTINUE_PROCESSING;
    }
}
