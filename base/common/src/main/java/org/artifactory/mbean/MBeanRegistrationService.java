package org.artifactory.mbean;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.context.ContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Set;

@Service
public class MBeanRegistrationService {
    private static final Logger log = LoggerFactory.getLogger(MBeanRegistrationService.class);

    private static final String MBEANS_DOMAIN_NAME = "org.jfrog.artifactory";
    private static final String MANAGED_PREFIX = "Managed";
    private static final String MBEAN_SUFFIX = "MBean";

    /**
     * Registers an object as a mbean.
     * <p><i>It is assumed that the given {@code mbean} has an interface with name ends with MBean</i></p>
     *
     * @param mbean      The mbean implementation
     */
    public <T> void register(T mbean) {
        register(mbean, createObjectName(getMBeanInterface(mbean), null));
    }

    /**
     * Registers an object as an mbean.
     * <p><i>It is assumed that the given {@code mbean} has an interface with name ends with MBean</i></p>
     *
     * @param mbean      The mbean implementation
     * @param mbeanProps Optional string to attach to the mbean name
     */
    public <T> void register(T mbean, @Nullable String mbeanProps) {
        register(mbean, createObjectName(getMBeanInterface(mbean), mbeanProps));
    }

    /**
     * Registers an object as an mbean.
     *
     * @param mbean      The mbean implementation
     * @param mbeanIfc   The mbean interface
     * @param mbeanProps Optional string to attach to the mbean name
     */
    public <T> void register(T mbean, Class<T> mbeanIfc, @Nullable String mbeanProps) {
        register(mbean, createObjectName(mbeanIfc, mbeanProps));
    }

    public <T> void register(T mbean, String group, @Nullable String prop) {
        register(mbean, createObjectName(group, prop));
    }

    private <T> void register(T mbean, ObjectName mbeanName) {
        try {
            if (getMBeanServer().isRegistered(mbeanName)) {
                log.debug("Unregistering existing mbean '{}'.", mbeanName);
                getMBeanServer().unregisterMBean(mbeanName);
            }
            log.debug("Registering mbean '{}'.", mbeanName);
            getMBeanServer().registerMBean(mbean, mbeanName);
        } catch (Exception e) {
            throw new RuntimeException("Could not register new mbean '" + mbeanName + "'.", e);
        }
    }

    /**
     * Unregisters all MBeans where the type matches given {@code type}.
     * <p>Object name is created using {@link #createObjectName(String, String)}
     * <p><i>In case of exception, a warn log will be written </i>
     * @return  the count of unregistered MBeans
     * @param mbeanIfc
     */
    public int unregisterAll(String type) {
        int count = 0;
        String canonicalName = createObjectName(type, null).getCanonicalName();

        Set<ObjectInstance> objectInstances = getMBeanServer().queryMBeans(null, null);
        for (ObjectInstance objectInstance : objectInstances) {
            ObjectName objectName = objectInstance.getObjectName();
            if (objectName.getCanonicalName().startsWith(canonicalName)) {
                try {
                    getMBeanServer().unregisterMBean(objectName);
                    count++;
                } catch (Exception e) {
                    log.warn("Could not un-register MBean '" + objectName.getCanonicalName() + "'.", e.getMessage());
                }
            }
        }
        return count;
    }

    /**
     * Create MBean name for the given {@code mbeanIfc} according to the convention {@code prefix:instance:type:props}
     * <p>for example: {@code org.jfrog.artifactory:Artifactory:Repository:libs-releases-locel}
     * @param mbeanIfc   The mbean interface
     * @param mbeanProps Optional string to attach to the mbean name
     * @return  the MBean name for the given {@code mbeanIfc}
     */
    protected ObjectName createObjectName(Class mbeanIfc, @Nullable String mbeanProps) {
        String type = mbeanIfc.getSimpleName();
        if (type.startsWith(MANAGED_PREFIX)) {
            type = type.substring(MANAGED_PREFIX.length());
        }
        if (type.endsWith(MBEAN_SUFFIX)) {
            type = type.substring(0, type.length() - MBEAN_SUFFIX.length());
        }

        return createObjectName(type, mbeanProps);
    }

    protected ObjectName createObjectName(String type, String mbeanProps) {
        String instanceId = ContextHelper.get().getContextId();
        if (StringUtils.isBlank(instanceId)) {
            instanceId = "Artifactory"; //default instanceId for tomcat ROOT
        }
        String nameStr = MBEANS_DOMAIN_NAME + ":" + "instance=" + instanceId + ", type=" + type;
        if (StringUtils.isNotBlank(mbeanProps)) {
            nameStr += ",prop=" + mbeanProps;
        }

        try {
            return new ObjectName(nameStr);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("Failed to create object name for '" + nameStr + "'.", e);
        }
    }

    protected MBeanServer getMBeanServer() {
        //Delegate to the mbean server already created by the platform
        return ManagementFactory.getPlatformMBeanServer();
    }

    private static <T> Class<T> getMBeanInterface(T mbean) {
        for (Class<?> ifc : mbean.getClass().getInterfaces()) {
            if (ifc.getName().endsWith(MBEAN_SUFFIX)) {
                return (Class<T>) ifc;
            }
        }
        return null;
    }
}

