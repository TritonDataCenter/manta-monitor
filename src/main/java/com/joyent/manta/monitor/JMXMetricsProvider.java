package com.joyent.manta.monitor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.management.MBeanServerConnection;
import java.util.Set;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.List;
import javax.management.ObjectName;

public class JMXMetricsProvider {

    private final ConnectionProvider<JMXClient> jmxClientConnectionProvider;

    private final List<Double> arrayOfPutRequestMeanValue = new ArrayList<>();

    @Inject @Named("retryCount")
    private Long retryCount;

    @Inject
    public JMXMetricsProvider(ConnectionProvider<JMXClient> jmxClient) {
        this.jmxClientConnectionProvider = jmxClient;
    }

    public void recordMetrics() throws Exception {

        JMXClient jmxClient = jmxClientConnectionProvider.get();

        MBeanServerConnection mBeanServerConnection = jmxClient.getMBeanServerConnection();

        Set<ObjectName> names = new TreeSet<>(mBeanServerConnection.queryNames(null, null));
        for (ObjectName name : names) {
            if(name.getCanonicalName().startsWith("com.joyent.manta.client:00=requests-put")) {
                System.out.println("Mean time: "+mBeanServerConnection.getAttribute(name, "Mean"));
                arrayOfPutRequestMeanValue.add((Double)mBeanServerConnection.getAttribute(name, "Mean"));
            }

            if(name.getCanonicalName().startsWith("com.joyent.manta.client:00=retries")) {
                retryCount = (Long)(mBeanServerConnection.getAttribute(name, "Count"));
            }
        }

    }

    public Double getPutRequestMeanValue() {
        if(arrayOfPutRequestMeanValue.size() > 0) {
            return arrayOfPutRequestMeanValue.get(arrayOfPutRequestMeanValue.size()-1);
        } else {
           return null;
        }
    }

    public Long getRetryCount() {
        return retryCount;
    }
}
