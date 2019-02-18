/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor.chains;

import com.google.common.collect.ImmutableList;
import com.joyent.manta.monitor.HoneyBadgerRequestFactory;
import com.joyent.manta.monitor.InstanceMetadata;
import com.joyent.manta.monitor.commands.CleanupCommand;
import com.joyent.manta.monitor.commands.GenerateFileCommand;
import com.joyent.manta.monitor.commands.GetFileCommand;
import com.joyent.manta.monitor.commands.HeadFileCommand;
import com.joyent.manta.monitor.commands.MantaOperationCommand;
import com.joyent.manta.monitor.commands.MkdirCommand;
import com.joyent.manta.monitor.commands.MultipartPutFileCommand;
import io.honeybadger.reporter.NoticeReporter;

import javax.inject.Inject;

import static com.google.common.collect.ImmutableList.of;

/**
 * This chain is a group of {@link org.apache.commons.chain.Command} objects
 * that perform a mkdir, multipart file upload, get and delete.
 */
public class FileMultipartUploadGetDeleteChain extends MantaOperationsChain {
    private static final ImmutableList<MantaOperationCommand> COMMANDS = of(
            GenerateFileCommand.INSTANCE,
            MkdirCommand.INSTANCE,
            MultipartPutFileCommand.INSTANCE,
            HeadFileCommand.INSTANCE,
            GetFileCommand.INSTANCE,
            CleanupCommand.INSTANCE);

    @Inject
    public FileMultipartUploadGetDeleteChain(final NoticeReporter reporter,
                                             final HoneyBadgerRequestFactory requestFactory,
                                             final InstanceMetadata metadata) {
        super(COMMANDS, reporter, requestFactory, metadata);
    }
}
