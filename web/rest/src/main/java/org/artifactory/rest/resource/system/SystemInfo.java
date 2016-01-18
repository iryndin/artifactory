package org.artifactory.rest.resource.system;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author Gidi Shabat
 */
public class SystemInfo {
    private long committedVirtualMemorySize;
    private long totalSwapSpaceSize;
    private long freeSwapSpaceSize;
    private long processCpuTime;
    private long totalPhysicalMemorySize;
    private long openFileDescriptorCount;
    private long maxFileDescriptorCount;
    private double processCpuLoad;
    private double systemCpuLoad;
    private long freePhysicalMemorySize;
    private int numberOfCores;
    private long heapMemoryUsage;
    private long noneHeapMemoryUsage;
    private int threadCount;
    private long noneHeapMemoryMax;
    private long heapMemoryMax;

    public long getCommittedVirtualMemorySize() {
        return committedVirtualMemorySize;
    }

    public void setCommittedVirtualMemorySize(long committedVirtualMemorySize) {
        this.committedVirtualMemorySize = committedVirtualMemorySize;
    }

    public long getTotalSwapSpaceSize() {
        return totalSwapSpaceSize;
    }

    public void setTotalSwapSpaceSize(long totalSwapSpaceSize) {
        this.totalSwapSpaceSize = totalSwapSpaceSize;
    }

    public long getFreeSwapSpaceSize() {
        return freeSwapSpaceSize;
    }

    public void setFreeSwapSpaceSize(long freeSwapSpaceSize) {
        this.freeSwapSpaceSize = freeSwapSpaceSize;
    }

    public long getProcessCpuTime() {
        return processCpuTime;
    }

    public void setProcessCpuTime(long processCpuTime) {
        this.processCpuTime = processCpuTime;
    }

    public long getTotalPhysicalMemorySize() {
        return totalPhysicalMemorySize;
    }

    public void setTotalPhysicalMemorySize(long totalPhysicalMemorySize) {
        this.totalPhysicalMemorySize = totalPhysicalMemorySize;
    }

    public long getOpenFileDescriptorCount() {
        return openFileDescriptorCount;
    }

    public void setOpenFileDescriptorCount(long openFileDescriptorCount) {
        this.openFileDescriptorCount = openFileDescriptorCount;
    }

    public long getMaxFileDescriptorCount() {
        return maxFileDescriptorCount;
    }

    public void setMaxFileDescriptorCount(long maxFileDescriptorCount) {
        this.maxFileDescriptorCount = maxFileDescriptorCount;
    }

    public double getProcessCpuLoad() {
        return processCpuLoad;
    }

    public void setProcessCpuLoad(double processCpuLoad) {
        BigDecimal bd = new BigDecimal(processCpuLoad).setScale(3, RoundingMode.FLOOR);
        this.processCpuLoad = bd.doubleValue();
    }

    public double getSystemCpuLoad() {
        return systemCpuLoad;
    }

    public void setSystemCpuLoad(double systemCpuLoad) {
        BigDecimal bd = new BigDecimal(systemCpuLoad).setScale(3, RoundingMode.FLOOR);
        this.systemCpuLoad = bd.doubleValue();
    }

    public long getFreePhysicalMemorySize() {
        return freePhysicalMemorySize;
    }

    public void setFreePhysicalMemorySize(long freePhysicalMemorySize) {
        this.freePhysicalMemorySize = freePhysicalMemorySize;
    }

    public int getNumberOfCores() {
        return numberOfCores;
    }

    public void setNumberOfCores(int numberOfCores) {
        this.numberOfCores = numberOfCores;
    }

    public long getHeapMemoryUsage() {
        return heapMemoryUsage;
    }

    public void setHeapMemoryUsage(long heapMemoryUsage) {
        this.heapMemoryUsage = heapMemoryUsage;
    }

    public long getNoneHeapMemoryUsage() {
        return noneHeapMemoryUsage;
    }

    public void setNoneHeapMemoryUsage(long noneHeapMemoryUsage) {
        this.noneHeapMemoryUsage = noneHeapMemoryUsage;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public long getNoneHeapMemoryMax() {
        return noneHeapMemoryMax;
    }

    public long getHeapMemoryMax() {
        return heapMemoryMax;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public void setNoneHeapMemoryMax(long noneHeapMemoryMax) {
        this.noneHeapMemoryMax = noneHeapMemoryMax;
    }

    public void setHeapMemoryMax(long heapMemoryMax) {
        this.heapMemoryMax = heapMemoryMax;
    }
}
