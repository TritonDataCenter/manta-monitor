package com.joyent.manta.monitor.functions;

import org.apache.commons.codec.binary.Hex;
import org.testng.annotations.Test;

@Test
public class GeneratePathBasedOnSHA256Test {
    public void canGenerateDirectoryPath() throws Exception {
        final String rootDir = "/user/stor/files";
        final GeneratePathBasedOnSHA256 generator = new GeneratePathBasedOnSHA256(rootDir);

        final String sha256 = "85a23692b73fc9f14a92d8530efbb92024ffaae01a88854a4e71acee641ade92";
        final byte[] checksum = Hex.decodeHex(sha256.toCharArray());
        final String path = generator.apply(checksum);

        System.out.println(path);
    }
}
