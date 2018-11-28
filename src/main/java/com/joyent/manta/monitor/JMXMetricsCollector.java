package com.joyent.manta.monitor;

import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.management.*;

import javax.validation.ValidationException;

import java.io.InvalidObjectException;
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
    public <T extends Number> T getMBeanAttributeValue(String mBeanObjectName, String attribute, final Class<T> returnType) {
        ObjectName objectName = getObjectNameFromString(mBeanObjectName);
        if (objectName == null) {
            String message = "Requested MBean Object not found";
            Map<String, Object> context = new HashMap<>();
            context.put("objectName", mBeanObjectName);
            context.put("attribute", attribute);
            context.put("mbeanServerDomain", MBEAN_DOMAIN);
            context.put("mbeanObjectKey", MBEAN_OBJECT_KEY);
            context.put("expectedAttributeReturnType", returnType.getCanonicalName());
            MBeanServerOperationException mBeanServerOperationException = createMBeanServerOperationException(message, new InvalidObjectException(message), context);
            throw mBeanServerOperationException;
        }
        MBeanServer mBeanServer = platformMbeanServerProvider.getPlatformMBeanServer();
        Object value = null;
        try {
            value = mBeanServer.getAttribute(objectName, attribute);
            final Number number;

            if (value instanceof Number) {
                number = (Number) value;
            } else {
                try {
                    number = Double.parseDouble(value.toString());
                } catch (NumberFormatException e) {
                    String message = "Failed to parse attribute value to number";
                    Map<String, Object> context = new HashMap<>();
                    context.put("objectName", objectName.getCanonicalName());
                    context.put("attribute", attribute);
                    context.put("mbeanServerDomain", MBEAN_DOMAIN);
                    context.put("mbeanObjectKey", MBEAN_OBJECT_KEY);
                    context.put("attributeValue", value.toString());
                    context.put("expectedAttributeReturnType", returnType.getCanonicalName());
                    MBeanServerOperationException mBeanServerOperationException = createMBeanServerOperationException(message, e, context);
                    throw mBeanServerOperationException;
                }
            }

            if (returnType.equals(number.getClass())) {
                return (T)number;
            } else if (returnType.equals(Short.class)) {
                return (T)Short.valueOf(number.shortValue());
            } else if (returnType.equals(Integer.class)) {
                return (T)Integer.valueOf(number.intValue());
            } else if (returnType.equals(Long.class)) {
                return (T)Long.valueOf(number.longValue());
            } else if (returnType.equals(Float.class)) {
                return (T)Float.valueOf(number.floatValue());
            } else if (returnType.equals(Double.class)) {
                return (T)Double.valueOf(number.doubleValue());
            } else if (returnType.equals(Byte.class)) {
                return (T)Byte.valueOf(number.byteValue());
            }
        } catch (AttributeNotFoundException | InstanceNotFoundException | ReflectionException | MBeanException e) {
            Map<String, Object> context = new HashMap<>();
            String message;
            if (e instanceof AttributeNotFoundException) {
                message = "The specified attribute does not exist or cannot be retrieved";
            } else if (e instanceof  InstanceNotFoundException) {
                message = "The specified MBean does not exist in the repository.";
            } else if (e instanceof MBeanException) {
                message = "Failed to retrieve the attribute from the MBean Server.";
            } else {
                message = "The requested operation is not supported by the MBean Server ";
            }
            context.put("objectName", objectName.getCanonicalName());
            context.put("attribute", attribute);
            context.put("mbeanServerDomain", MBEAN_DOMAIN);
            context.put("mbeanObjectKey", MBEAN_OBJECT_KEY);
            context.put("attributeValue", value.toString());
            context.put("expectedAttributeReturnType", returnType.getCanonicalName());

            MBeanServerOperationException mBeanServerOperationException = createMBeanServerOperationException(message, e, context);
            throw mBeanServerOperationException;
        }
        return null;
    }

    public boolean validateMbeanObject(String objectName) {
        ObjectName mbeanObject = getObjectNameFromString(objectName);
        boolean response = false;
        if (mbeanObject != null) {
            MBeanServer mBeanServer = platformMbeanServerProvider.getPlatformMBeanServer();
            response =  mBeanServer.isRegistered(mbeanObject);
        }
        if (!response) {
            String message = "Requested mbean object is not registered with the Platform MBean Server";
            Map<String, Object> context = new HashMap<>();
            context.put("mbeanServerDomain", MBEAN_DOMAIN);
            context.put("mbeanObjectKey", MBEAN_OBJECT_KEY);
            context.put("objectName", objectName);
            MBeanServerOperationException mBeanServerOperationException = createMBeanServerOperationException(message, new ValidationException(message), context);
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
            context.put("mbeanServerDomain", MBEAN_DOMAIN);
            context.put("mbeanObjectKey", MBEAN_OBJECT_KEY);
            context.put("objectName", objectName);
            MBeanServerOperationException mBeanServerOperationException = createMBeanServerOperationException(message, mfe, context);
            throw mBeanServerOperationException;
        }
        return responseObjectName;
    }

    private MBeanServerOperationException createMBeanServerOperationException(String message, Throwable cause, Map<String, Object> context) {
        MBeanServerOperationException mBeanServerOperationException;
        if (cause == null) {
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
