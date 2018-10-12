package com.joyent.manta.monitor;

import javax.management.MBeanServerConnection;


public class JMXClient {

    private final MBeanServerConnection mBeanServerConnection;

    JMXClient(MBeanServerConnection mBeanServerConnection) {
        this.mBeanServerConnection = mBeanServerConnection;
    }

    public MBeanServerConnection getMBeanServerConnection() {
        return mBeanServerConnection;
    }

}
