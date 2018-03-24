package com.joyent.manta.monitor.chains;

import com.joyent.manta.monitor.commands.MantaOperationCommand;
import com.joyent.manta.monitor.commands.*;
import io.honeybadger.reporter.HoneybadgerUncaughtExceptionHandler;

import java.util.List;

import static com.google.common.collect.ImmutableList.*;

public class FileUploadGetDeleteChain extends MantaOperationsChain {

    private static final List<MantaOperationCommand> COMMANDS = of(
            GenerateFileCommand.INSTANCE,
            MkdirCommand.INSTANCE,
            PutFileCommand.INSTANCE,
            GetFileCommand.INSTANCE,
            DeleteFileCommand.INSTANCE);

    public FileUploadGetDeleteChain(final Thread.UncaughtExceptionHandler exceptionHandler) {
        super(COMMANDS, exceptionHandler);
    }
}
