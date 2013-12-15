package org.artifactory.converters;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Properties;

/**
 * Conversion lock: Use it to avoid simultaneous conversion, by two or more servers.
 * Please note the class is not thread save (currently is is being used by single thread)
 *
 * @author: gidis
 */
public class ConversionLock {
    private static final Logger log = LoggerFactory.getLogger(ConversionLock.class);
    private ArtifactoryHome artifactoryHome;
    private VersionProviderImpl vm;
    private boolean lock;

    public ConversionLock(ArtifactoryHome artifactoryHome, VersionProviderImpl vm) {
        this.artifactoryHome = artifactoryHome;
        this.vm = vm;
    }

    public void lock() {
        // If lock is already obtained then return successfully
        if (lock) {
            return;
        }
        // Check if the lock is not obtained by other server
        if (!isLockFileExists()) {
            Properties properties = new Properties();
            properties.put("lockTime", "" + new Date().getTime());
            properties.put("homeOriginalVersion", vm.getOriginalHaVersionDetails().getVersion().getValue());
            properties.put("clusterOriginalVersion", vm.getOriginalHaVersionDetails().getVersion().getValue());
            properties.put("runningOriginalVersion", vm.getRunningVersionDetails().getVersion().getValue());
            CompoundVersionDetails originalServiceVersion = vm.getOriginalServiceVersionDetails();
            String value = originalServiceVersion != null ? originalServiceVersion.getVersion().getValue() : "notReady";
            properties.put("servicesOriginalVersion", value);
            File homeConversionLockFile = artifactoryHome.getHomeConversionLockFile();
            File haConversionLockFile = artifactoryHome.getHaConversionLockFile();
            if (artifactoryHome.isHaConfigured()) {
                saveLockFile(properties, haConversionLockFile);
            }
            saveLockFile(properties, homeConversionLockFile);
            lock = true;
        } else {
            throw new RuntimeException("Conversion lock can't obtain the lock,it has been locked by another server");
        }
    }

    public boolean unlock() {
        if (isMyLock()) {
            File homeConversionLockFile = artifactoryHome.getHomeConversionLockFile();
            File haConversionLockFile = artifactoryHome.getHaConversionLockFile();
            if (artifactoryHome.isHaConfigured()) {
                deleteLockFile(haConversionLockFile);
            }
            deleteLockFile(homeConversionLockFile);
            lock = false;
        }
        return false;
    }

    boolean isLockFileExists() {
        File homeConversionLockFile = artifactoryHome.getHomeConversionLockFile();
        File haConversionLockFile = artifactoryHome.getHaConversionLockFile();
        boolean homeExists = homeConversionLockFile.exists();
        boolean haExists = haConversionLockFile.exists();
        return artifactoryHome.isHaConfigured() && haExists || homeExists;
    }

    private void saveLockFile(Properties properties, File file) {
        try (FileOutputStream fis = new FileOutputStream(file)) {
            properties.store(fis, "");
        } catch (Exception e) {
            throw new RuntimeException("Fail to create conversion lock file " + file.getPath());
        }
    }

    private void deleteLockFile(File file) {
        boolean delete = file.delete();
        if (!file.exists()) {
            log.warn("Couldn't delete lock file the file doesn't exists");
            return;
        }
        if (!delete) {
            throw new RuntimeException("Fail to delete conversion lock file " + file.getPath());
        }
    }

    public boolean isMyLock() {
        return lock && isLockFileExists();
    }

}
