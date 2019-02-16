/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.functions;

import org.apache.commons.codec.binary.Hex;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class GeneratePathBasedOnSHA256Test {
    public void canGenerateDirectoryPath() throws Exception {
        final String rootDir = "/user/stor/files";
        final GeneratePathBasedOnSHA256 generator = new GeneratePathBasedOnSHA256(rootDir);

        final String sha256 = "85a23692b73fc9f14a92d8530efbb92024ffaae01a88854a4e71acee641ade92";
        final byte[] checksum = Hex.decodeHex(sha256.toCharArray());
        final String path = generator.apply(checksum);

        Assert.assertEquals(path, "/user/stor/files/0/1/0/9/b/3/c/",
                "Expected directory structure wasn't created");
    }
}
