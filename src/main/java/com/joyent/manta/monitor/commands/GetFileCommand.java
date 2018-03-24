package com.joyent.manta.monitor.commands;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaObjectInputStream;
import com.joyent.manta.monitor.MantaOperationContext;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;

public class GetFileCommand implements MantaOperationCommand {
    public static final GetFileCommand INSTANCE = new GetFileCommand();

    @Override
    public boolean execute(final MantaOperationContext context) throws Exception {
        final MantaClient client = context.getMantaClient();
        final String filePath = context.getFilePath();
        final MessageDigest checksum = MessageDigest.getInstance("SHA256");

        try (MantaObjectInputStream in = client.getAsInputStream(filePath);
             DigestInputStream digestIn = new DigestInputStream(in, checksum)) {

            final byte[] buf = new byte[8192];
            while (digestIn.read(buf) > -1);
        }

        final byte[] sha256 = checksum.digest();

        if (!Arrays.equals(sha256, context.getTestFileChecksum())) {
            String msg = "The checksum for the file uploaded and file downloaded "
                    + "do not match";
            throw new IllegalStateException(msg);
        }

        return CONTINUE_PROCESSING;
    }
}
