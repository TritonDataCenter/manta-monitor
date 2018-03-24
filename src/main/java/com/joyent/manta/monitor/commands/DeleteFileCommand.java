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
