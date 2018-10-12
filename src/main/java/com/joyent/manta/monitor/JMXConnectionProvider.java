package com.joyent.manta.monitor;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;

public class JMXConnectionProvider implements ConnectionProvider<JMXClient> {

    @Override
    public JMXClient get() throws IOException {
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:9010/jmxrmi");
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        MBeanServerConnection mBeanServerConnection = jmxc.getMBeanServerConnection();
        return new JMXClient(mBeanServerConnection);
    }
}
