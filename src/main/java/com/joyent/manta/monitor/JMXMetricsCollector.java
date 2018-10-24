package com.joyent.manta.monitor;

import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.management.ObjectName;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class JMXMetricsCollector {
    private final PlatformMbeanServerProvider platformMbeanServerProvider;

    private static final String MBEAN_DOMAIN = "com.joyent.manta.client";
    private static final String MBEAN_OBJECT_KEY = "00";

    @Inject
    public JMXMetricsCollector(PlatformMbeanServerProvider platformMbeanServerProvider) {
        this.platformMbeanServerProvider = platformMbeanServerProvider;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private <T extends Number> T getMBeanAttributeValue(String mBeanObjectName, String attribute, final Class<T> returnType) throws Exception {
        ObjectName objectName = getObjectNameFromString(mBeanObjectName);
        if (objectName != null) {
            MBeanServer mBeanServer = platformMbeanServerProvider.getPlatformMBeanServer();
            try {
                if(returnType.equals(Double.class)) {
                    Double d = new Double((mBeanServer.getAttribute(objectName, attribute)).toString());
                    return (T) d;
                } else if(returnType.equals(Long.class)) {
                    Long l = new Long((mBeanServer.getAttribute(objectName, attribute)).toString());
                    return (T) l;
                }
            } catch (Exception e) {
                String message = "Failed to get attribute value from the platform mbeanServer";
                Map<String, Object> context = new HashMap<>();
                context.put("objectName", objectName.getCanonicalName());
                context.put("attribute", attribute);
                MBeanServerOperationException mBeanServerOperationException = createMBeanServerOperationException(message, e, context);
                throw mBeanServerOperationException;
            }
        }
        return null;
    }

    @Nullable
    public Double getPutRequestMeanValue() throws Exception {
        Double value = getMBeanAttributeValue("requests-put", "Mean", Double.class);
        if(value != null) {
            return value;
        } else {
            String message = "Error in getting attribute value";
            Map<String, Object> context = new HashMap<>();
            context.put("RequestedMBeanObject", "requests-put");
            context.put("RequestedAttribute", "Mean");
            MBeanServerOperationException mBeanServerOperationException = createMBeanServerOperationException(message, null, context);
            throw mBeanServerOperationException;
        }
    }

    @Nullable
    public Double getPutRequest50thPercentileValue() throws Exception {
        Double value = getMBeanAttributeValue("requests-put", "50thPercentile", Double.class);
        if(value != null) {
            return value;
        } else {
            String message = "Error in getting attribute value";
            Map<String, Object> context = new HashMap<>();
            context.put("RequestedMBeanObject", "requests-put");
            context.put("RequestedAttribute", "50thPercentile");
            MBeanServerOperationException mBeanServerOperationException = createMBeanServerOperationException(message, null, context);
            throw mBeanServerOperationException;
        }
    }

    @Nullable
    public Long getRetryCount() throws Exception {
        Long value = getMBeanAttributeValue("retries", "Count", Long.class);
        if(value != null) {
            return value;
        } else {
            String message = "Error in getting attribute value";
            Map<String, Object> context = new HashMap<>();
            context.put("RequestedMBeanObject", "retries");
            context.put("RequestedAttribute", "Count");
            MBeanServerOperationException mBeanServerOperationException = createMBeanServerOperationException(message, null, context);
            throw mBeanServerOperationException;
        }
    }

    @Nullable
    public Long getPutRequestCount() throws Exception {
        Long value = getMBeanAttributeValue("requests-put", "Count", Long.class);
        if(value != null) {
            return value;
        } else {
            String message = "Error in getting attribute value";
            Map<String, Object> context = new HashMap<>();
            context.put("RequestedMBeanObject", "requests-put");
            context.put("RequestedAttribute", "Count");
            MBeanServerOperationException mBeanServerOperationException = createMBeanServerOperationException(message, null, context);
            throw mBeanServerOperationException;
        }
    }

    public boolean validateMbeanObject(String objectName) throws Exception {
        ObjectName mbeanObject = getObjectNameFromString(objectName);;
        boolean response = false;
        if(mbeanObject != null) {
            MBeanServer mBeanServer = platformMbeanServerProvider.getPlatformMBeanServer();
            response =  mBeanServer.isRegistered(mbeanObject);
        }
        if(!response) {
            String message = "Requested mbean object is not registered with the platform mbean server";
            Map<String, Object> context = new HashMap<>();
            context.put("MBeanDomain", MBEAN_DOMAIN);
            context.put("MBeanObjectKey", MBEAN_OBJECT_KEY);
            context.put("RequestedMBeanObject", objectName);
            context.put("MBeanObjectFromPlatform", mbeanObject);
            MBeanServerOperationException mBeanServerOperationException = createMBeanServerOperationException(message, null, context);
            throw mBeanServerOperationException;
        }
        return response;
    }

    @Nullable
    private ObjectName getObjectNameFromString(String objectName) {
        String objectNameWithDomain = MBEAN_DOMAIN+":"+MBEAN_OBJECT_KEY+"="+objectName+",*";
        MBeanServer mBeanServer =platformMbeanServerProvider.getPlatformMBeanServer();
        ObjectName responseObjectName = null;
        try {
            ObjectName mbeanObjectName = new ObjectName(objectNameWithDomain);
            Set<ObjectName> objectNames = mBeanServer.queryNames(mbeanObjectName, null);
            for(ObjectName object: objectNames) {
                responseObjectName = object;
            }
        } catch (MalformedObjectNameException mfe) {
            String message = String.format("Error in creating mbean object name from the string %s", objectName);
            Map<String, Object> context = new HashMap<>();
            context.put("MBeanDomain", MBEAN_DOMAIN);
            context.put("MBeanObjectKey", MBEAN_OBJECT_KEY);
            context.put("MBeanObject", objectName);
            MBeanServerOperationException mBeanServerOperationException = createMBeanServerOperationException(message, mfe, context);
            throw mBeanServerOperationException;
        }
        return responseObjectName;
    }

    private MBeanServerOperationException createMBeanServerOperationException(String message, Throwable cause, Map<String, Object> context) {
        MBeanServerOperationException mBeanServerOperationException;
        if(cause == null) {
            mBeanServerOperationException = new MBeanServerOperationException(message);
        } else {
            mBeanServerOperationException = new MBeanServerOperationException(message, cause);
        }
        context.forEach((label, value) -> {
            mBeanServerOperationException.setContextValue(label, value);
        });
        return mBeanServerOperationException;
    }
}
