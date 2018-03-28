package com.joyent.manta.monitor;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.monitor.chains.ChainRunner;
import com.joyent.manta.monitor.chains.FileUploadGetDeleteChain;
import io.honeybadger.reporter.HoneybadgerUncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;

public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    private static final Provider<MantaClient> mantaClientProvider =
            new MantaClientProvider();

    static {
        HoneybadgerUncaughtExceptionHandler.registerAsUncaughtExceptionHandler();
    }

    public static void main(String[] args) throws InterruptedException {
        MantaClient client = mantaClientProvider.get();
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
                Thread.currentThread().getUncaughtExceptionHandler();
        FileUploadGetDeleteChain chain = new FileUploadGetDeleteChain(uncaughtExceptionHandler);
        ChainRunner runner = new ChainRunner(chain, "simple-put",
                5, client, uncaughtExceptionHandler);

        runner.start();

        while (runner.isRunning()) {
            Thread.currentThread().wait();
        }
    }
}
