/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.commands;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class MultipartPutFileCommandTest {
    private static final int MINIMUM_PART_SIZE = 5_242_880; // 5 mebibytes

    public void canCalculateChunkSizesForNumberWithNoRemainder() {
        final long fileSize = 104857600; // 100 mebibytes
        final int totalParts = 10;
        final long expectedChunkSize = fileSize / totalParts;
        final long[] chunkSizes = MultipartPutFileCommand.calculateChunkSizes(
                fileSize, totalParts, MINIMUM_PART_SIZE);

        Assert.assertEquals(chunkSizes.length, totalParts,
                "Number of chunk sizes should correspond to total parts");

        long totalSize = 0;
        for (final long chunkSize : chunkSizes) {
            totalSize += chunkSize;
            Assert.assertEquals(chunkSize, expectedChunkSize);
        }

        Assert.assertEquals(totalSize, fileSize);
    }

    public void canCalculateChunkSizesForNumberWithRemainder() {
        final long fileSize = 104857603; // 100 mebibytes + 3
        final int totalParts = 10;
        final long[] chunkSizes = MultipartPutFileCommand.calculateChunkSizes(
                fileSize, totalParts, MINIMUM_PART_SIZE);

        Assert.assertEquals(chunkSizes.length, totalParts,
                "Number of chunk sizes should correspond to total parts");

        long totalSize = 0;
        for (final long chunkSize : chunkSizes) {
            totalSize += chunkSize;
        }

        Assert.assertEquals(totalSize, fileSize);
    }
}
