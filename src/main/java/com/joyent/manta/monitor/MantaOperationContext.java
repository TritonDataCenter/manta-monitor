package com.joyent.manta.monitor;

import com.joyent.manta.client.MantaClient;
import org.apache.commons.chain.Context;
import org.apache.commons.codec.binary.Hex;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("unchecked")
public class MantaOperationContext extends ConcurrentHashMap implements Context {
    private static final String MANTA_CLIENT_KEY = "mantaClient";
    private static final String MIN_FILE_SIZE_KEY = "minFileSize";
    private static final String MAX_FILE_SIZE_KEY = "maxFileSize";
    private static final String FILE_PATH_GEN_FUNC_KEY = "filePathGenerationFunction";
    private static final String FILE_PATH_KEY = "filePath";
    private static final String TEST_FILE_KEY = "testFile";
    private static final String TEST_FILE_CHECKSUM_KEY = "testFileChecksum";
    private static final String TEST_FILE_CHECKSUM_AS_STRING_KEY = "testFileChecksumString";
    private static final String RESPONSE_TIMES_KEY = "responseTime";

    public MantaOperationContext() {
        super();
        put(RESPONSE_TIMES_KEY, new ConcurrentHashMap<UUID, Integer>());
    }

    @Override
    public void clear() {
        super.clear();
        put(RESPONSE_TIMES_KEY, new ConcurrentHashMap<UUID, Integer>());
    }

    public MantaClient getMantaClient() {
        return (MantaClient)get(MANTA_CLIENT_KEY);
    }

    public MantaOperationContext setMantaClient(final MantaClient mantaClient) {
        put(MANTA_CLIENT_KEY, requireNonNull(mantaClient));
        return this;
    }
    
    public Integer getMinFileSize() {
        return (Integer)get(MIN_FILE_SIZE_KEY);
    }
    
    public MantaOperationContext setMinFileSize(final int minFileSize) {
        put(MIN_FILE_SIZE_KEY, minFileSize);
        return this;
    }

    public Integer getMaxFileSize() {
        return (Integer)get(MAX_FILE_SIZE_KEY);
    }

    public MantaOperationContext setMaxFileSize(final int maxFileSize) {
        put(MAX_FILE_SIZE_KEY, maxFileSize);
        return this;
    }

    public Function<byte[], String> getFilePathGenerationFunction() {
        return (Function<byte[], String>)get(FILE_PATH_GEN_FUNC_KEY);
    }

    public MantaOperationContext setFilePathGenerationFunction(final Function<byte[], String> function) {
        put(FILE_PATH_GEN_FUNC_KEY, requireNonNull(function));
        return this;
    }

    public String getFilePath() {
        return (String)get(FILE_PATH_KEY);
    }

    public MantaOperationContext setFilePath(final String path) {
        put(FILE_PATH_KEY, requireNonNull(path));
        return this;
    }

    public Path getTestFile() {
        return (Path)get(TEST_FILE_KEY);
    }

    public MantaOperationContext setTestFile(final Path testFile){
        put(TEST_FILE_KEY, requireNonNull(testFile));
        return this;
    }

    public byte[] getTestFileChecksum() {
        return (byte[])get(TEST_FILE_CHECKSUM_KEY);
    }

    public MantaOperationContext setTestFileChecksum(final byte[] checksum) {
        put(TEST_FILE_CHECKSUM_KEY, requireNonNull(checksum));
        put(TEST_FILE_CHECKSUM_AS_STRING_KEY, Hex.encodeHexString(checksum));
        return this;
    }

    public String getTestFileChecksumAsString() {
        return (String)get(TEST_FILE_CHECKSUM_AS_STRING_KEY);
    }

    public Map<UUID, Integer> getResponseTimes() {
        return (Map<UUID, Integer>)get(RESPONSE_TIMES_KEY);
    }
}
