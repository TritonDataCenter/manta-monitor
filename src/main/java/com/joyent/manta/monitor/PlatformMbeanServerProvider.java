/*
 * Copyright (c) 2018, Joyent, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.joyent.manta.monitor;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;

/**
 * Class that provides the platform mbean server to enable retrieving JMX metrics provided the manta client.
 */
public class PlatformMbeanServerProvider {
    public MBeanServer getPlatformMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }
}
