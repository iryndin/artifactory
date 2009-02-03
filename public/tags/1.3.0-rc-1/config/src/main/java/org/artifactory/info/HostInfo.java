package org.artifactory.info;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

/**
 * An information group for all the host properties
 *
 * @author Noam Tenne
 */
public class HostInfo extends BaseInfoGroup {

    /**
     * Property map
     */
    TreeMap<String, String> propertyMap;
    /**
     * Operating system bean
     */
    OperatingSystemMXBean systemBean;
    /**
     * Memory bean
     */
    MemoryMXBean memoryBean;

    /**
     * Main constructor
     */
    public HostInfo() {
        //Get operating system bean
        systemBean = ManagementFactory.getOperatingSystemMXBean();
        //Get memory bean
        memoryBean = ManagementFactory.getMemoryMXBean();
        setPropertyMap();
    }

    /**
     * Sets the map with all the host property names and values
     */
    private void setPropertyMap() {
        propertyMap = new TreeMap<String, String>();
        propertyMap.put("os.arch", systemBean.getArch());
        propertyMap.put("os.name", systemBean.getName());
        propertyMap.put("os.version", systemBean.getVersion());
        propertyMap
                .put("Available Processors", Integer.toString(systemBean.getAvailableProcessors()));
        MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
        propertyMap
                .put("Heap Memory Usage-Commited", Long.toString(heapMemoryUsage.getCommitted()));
        propertyMap.put("Heap Memory Usage-Init", Long.toString(heapMemoryUsage.getInit()));
        propertyMap.put("Heap Memory Usage-Max", Long.toString(heapMemoryUsage.getMax()));
        propertyMap.put("Heap Memory Usage-Used", Long.toString(heapMemoryUsage.getUsed()));
        MemoryUsage nonHeapMemoryUsage = memoryBean.getNonHeapMemoryUsage();
        propertyMap.put("Non-Heap Memory Usage-Commited",
                Long.toString(nonHeapMemoryUsage.getCommitted()));
        propertyMap.put("Non-Heap Memory Usage-Init", Long.toString(nonHeapMemoryUsage.getInit()));
        propertyMap.put("Non-Heap Memory Usage-Max", Long.toString(nonHeapMemoryUsage.getMax()));
        propertyMap.put("Non-Heap Memory Usage-Used", Long.toString(nonHeapMemoryUsage.getUsed()));
    }

    /**
     * Returns all the info objects from the current group
     *
     * @return InfoObject[] - Collection of info objects from current group
     */
    @Override
    public InfoObject[] getInfo() {
        ArrayList<InfoObject> infoList = new ArrayList<InfoObject>();

        Set<String> keys = propertyMap.keySet();
        for (String key : keys) {
            InfoObject infoObjcet = new InfoObject(key, propertyMap.get(key));
            infoList.add(infoObjcet);
        }
        return infoList.toArray(new InfoObject[infoList.size()]);
    }
}