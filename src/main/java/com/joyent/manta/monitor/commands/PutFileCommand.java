package com.joyent.manta.monitor.commands;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaMetadata;
import com.joyent.manta.client.MantaObjectResponse;
import com.joyent.manta.http.MantaHttpHeaders;
import com.joyent.manta.monitor.MantaOperationContext;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.Objects;
import java.util.UUID;

public class PutFileCommand implements MantaOperationCommand {
    public static final PutFileCommand INSTANCE = new PutFileCommand();

    @Override
    public boolean execute(final MantaOperationContext context) throws Exception {
        final MantaClient client = context.getMantaClient();
        final String filePath = generateFilePath(context);
        context.setFilePath(filePath);

        final File file = context.getTestFile().toFile();

        try {
            final MantaHttpHeaders headers = new MantaHttpHeaders();
            headers.setContentType("text/plain; charset=UTF-8");
            final MantaMetadata metadata = new MantaMetadata();
            metadata.put("m-sha256-checksum", context.getTestFileChecksumAsString());

            /* Record the latencies per PUT operation so that we can act upon
             * pathological latency numbers. */
            final MantaObjectResponse response = client.put(
                    filePath, file, headers, metadata);
            final UUID requestId = UUID.fromString(response.getRequestId());
            final Integer responseTime = parseResponseTime(response.getHttpHeaders());
            context.getResponseTimes().put(requestId, responseTime);
        } finally {
            Files.deleteIfExists(context.getTestFile());
        }

        return CONTINUE_PROCESSING;
    }

    private static String generateFilePath(final  MantaOperationContext context) {
        final byte[] checksum = context.getTestFileChecksum();
        final String dir = context.getFilePathGenerationFunction().apply(checksum);

        return dir + context.getTestFileChecksumAsString() + ".txt";
    }

    private static Integer parseResponseTime(final MantaHttpHeaders headers) {
        final String responseTimeAsString = Objects.toString(headers.get("x-response-time"));

        if (StringUtils.isBlank(responseTimeAsString)) {
            return null;
        }

        return Integer.parseInt(responseTimeAsString);
    }
}
