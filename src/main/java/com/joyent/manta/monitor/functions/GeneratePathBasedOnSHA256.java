package com.joyent.manta.monitor.functions;

import java.util.function.Function;

import static com.joyent.manta.client.MantaClient.SEPARATOR;

public class GeneratePathBasedOnSHA256 implements Function<byte[], String> {
    private static final char[] DIGITS =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private final String rootDirectory;

    public GeneratePathBasedOnSHA256(final String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public String apply(final byte[] checksum) {
        final StringBuilder builder = new StringBuilder(rootDirectory)
                .append(SEPARATOR);

        for (int i = 0; i < checksum.length; i++) {
            final byte b = checksum[i];
            final char one = DIGITS[(0xF0 & b) >>> 4];
            final char two = DIGITS[0x0F & b];
            builder.append(one).append(two);

            if ( (i+1) % 2 == 0) {
                builder.append(SEPARATOR);
            }
        }

        return builder.toString();
    }
}
