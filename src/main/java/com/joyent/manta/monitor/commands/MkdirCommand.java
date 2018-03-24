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
