/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.functions;

import java.util.function.Function;

import static com.joyent.manta.client.MantaClient.SEPARATOR;

/**
 * Class that generates file path based on SHA256, to be used to execute the {@link com.joyent.manta.monitor.commands}.
 */
public class GeneratePathBasedOnSHA256 implements Function<byte[], String> {
    private static final char[] DIGITS =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private final String rootDirectory;

    /**
     * The maximum number of directories to create.
     */
    private static final int DIR_DEPTH = 7;

    /**
     * The number of directories on the path from leftmost side to ensure only
     * have a limited repeating set of names.
     */
    private static final int LIMITED_DIR_COUNT_DEPTH = 3;

    public GeneratePathBasedOnSHA256(final String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method creates directory structures like:</p>
     * <pre>/user/stor/manta-monitor-data/FileUploadGetDeleteChain/1/0/1/9/a/5/8</pre>
     */
    @Override
    public String apply(final byte[] checksum) {
        final StringBuilder builder = new StringBuilder(rootDirectory)
                .append(SEPARATOR);

        for (int i = 0; i < DIR_DEPTH; i++) {
            final byte b = checksum[i];
            final char firstCharacter = DIGITS[(0xF0 & b) >>> 4];

            /* Create a very repeatable directory names if we are still under
             * the limited dir count depth */
            if (i < LIMITED_DIR_COUNT_DEPTH) {
                builder.append(Math.abs(b % 3));
            /* Otherwise, we create single character directories that have some
             * amount of repeatability because they will always be a single
             * character name of the set of characters: 0123456789abcdef
             * If the chain is running multi-threaded, it is likely that you
             * will see some directories with multiple entries at some point. */
            } else {
                builder.append(firstCharacter);
            }

            builder.append(SEPARATOR);
        }

        return builder.toString();
    }
}
