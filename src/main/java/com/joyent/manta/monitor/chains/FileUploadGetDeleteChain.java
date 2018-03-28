/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.chains;

import com.joyent.manta.monitor.HoneyBadgerRequestFactory;
import com.joyent.manta.monitor.commands.MantaOperationCommand;
import com.joyent.manta.monitor.commands.*;
import io.honeybadger.reporter.NoticeReporter;

import javax.inject.Inject;
import java.util.List;

import static com.google.common.collect.ImmutableList.*;

public class FileUploadGetDeleteChain extends MantaOperationsChain {

    private static final List<MantaOperationCommand> COMMANDS = of(
            GenerateFileCommand.INSTANCE,
            MkdirCommand.INSTANCE,
            PutFileCommand.INSTANCE,
            GetFileCommand.INSTANCE,
            DeleteFileCommand.INSTANCE);

    @Inject
    public FileUploadGetDeleteChain(final NoticeReporter reporter,
                                    final HoneyBadgerRequestFactory requestFactory) {
        super(COMMANDS, reporter, requestFactory);
    }
}
