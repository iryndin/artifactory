package org.artifactory.converters;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.property.ArtifactoryConverter;
import org.artifactory.file.lock.LockFile;
import org.artifactory.file.lock.LockFileForNoneLockingFileSystem;
import org.artifactory.storage.db.properties.model.DbProperties;
import org.artifactory.storage.db.properties.service.ArtifactoryCommonDbPropertiesService;
import org.artifactory.storage.db.servers.service.ArtifactoryServersCommonService;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * The class manages the conversions process during Artifactory life cycle.
 * It creates a complete separation between the HOME, CLUSTER and database
 * environments, thoughts Artifactory converts each environment independently,
 * each environment has its own original version and each original version might
 * trigger the relevant conversion.
 *
 * @author Gidi Shabat
 */
public class ConvertersManagerImpl implements ConverterManager {
    private static final Logger log = LoggerFactory.getLogger(ConvertersManagerImpl.class);

    private final ArtifactoryHome artifactoryHome;
    private final VersionProviderImpl vp;
    private final LockFile conversionLock;
    private List<ArtifactoryConverterAdapter> localHomeConverters = new ArrayList<>();
    private List<ArtifactoryConverterAdapter> clusterHomeConverters = new ArrayList<>();
    private boolean serviceConversionStarted = false;

    public ConvertersManagerImpl(ArtifactoryHome artifactoryHome, VersionProviderImpl vp) {
        // Initialize
        this.artifactoryHome = artifactoryHome;
        this.vp = vp;
        // create home converters
        localHomeConverters.add(new LoggingConverter(artifactoryHome.getEtcDir()));
        localHomeConverters.add(new MimeTypeConverter(artifactoryHome.getMimeTypesFile()));
        // create cluster converters
        clusterHomeConverters.add(new MimeTypeConverter(artifactoryHome.getHaAwareMimeTypesFile()));
        // We need to synchronize conversion only in ha mode therefore use real lock file in ha environment
        if (artifactoryHome.isHaConfigured()) {
            conversionLock = new LockFileForNoneLockingFileSystem(artifactoryHome.getHaConversionLockFile());
        } else {
            conversionLock = new DummyLockFile();
        }
    }

    public void convertHomes() {
        if (isLocalHomeInterested() || isHaInterested()) {
            conversionLock.tryLock();
            convertLocalHome();
            convertCluster();
        }
    }

    private void convertLocalHome() {
        try {
            if (isLocalHomeInterested()) {
                CompoundVersionDetails originalHome = vp.getOriginalHome();
                CompoundVersionDetails running = vp.getRunning();
                String message = "Starting home conversion, from {}, to {}";
                log.info(message, originalHome.getVersion(), running.getVersion());
                runConverters(localHomeConverters, originalHome, running);
                log.info("Finished home conversion");
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void convertCluster() {
        try {
            if (isHaInterested()) {
                CompoundVersionDetails originalHa = vp.getOriginalHa();
                CompoundVersionDetails running = vp.getRunning();
                String message = "Starting cluster home conversion, from {}, to {}";
                log.info(message, originalHa.getVersion(), running.getVersion());
                runConverters(clusterHomeConverters, originalHa, running);
                log.info("Finished cluster home conversion");
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    @Override
    public void serviceConvert(ArtifactoryConverter artifactoryConverter) {
        try {
            if (isServiceConversionInterested()) {
                CompoundVersionDetails running = vp.getRunning();
                CompoundVersionDetails originalService = vp.getOriginalService();
                log.debug("Starting ReloadableBean conversion for: {}, from {} to {}",
                        artifactoryConverter.getClass().getName(), originalService, running);
                if (vp.getRunning().getVersion().beforeOrEqual(ArtifactoryVersion.v310)) {
                    assertIfNoOtherActiveServers();
                }
                conversionLock.tryLock();
                artifactoryConverter.convert(originalService, running);
                log.debug("Finished ReloadableBean conversion for: {}", artifactoryConverter.getClass().getName());
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    @Override
    public void conversionFinished() {
        if (conversionLock.isLockedByMe()) {
            try {
                // Save home artifactory.properties
                artifactoryHome.writeBundledHomeArtifactoryProperties();
                vp.reloadArtifactorySystemProperties(artifactoryHome.getHomeArtifactoryPropertiesFile());
                // Save cluster artifactory.properties
                if (artifactoryHome.isHaConfigured()) {
                    artifactoryHome.writeBundledHaArtifactoryProperties();
                    vp.reloadArtifactorySystemProperties(artifactoryHome.getHaArtifactoryPropertiesFile());
                }
                //Insert the new version to the database only if have to
                ArtifactoryCommonDbPropertiesService dbPropertiesService = ContextHelper.get().beanForType(
                        ArtifactoryCommonDbPropertiesService.class);
                if (isServiceConversionInterested()) {
                    dbPropertiesService.updateDbProperties(createDbPropertiesFromVersion(vp.getRunning()));
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to finish conversion", e);
            } finally {
                conversionLock.release();
            }
        }
    }

    private void handleException(Exception e) {
        log.error("Conversion failed. You should analyze the error and retry launching " +
                "Artifactory. Error is: {}", e.getMessage());
        conversionLock.release();
        throw new RuntimeException(e.getMessage(), e);
    }

    private DbProperties createDbPropertiesFromVersion(CompoundVersionDetails versionDetails) {
        long installTime = System.currentTimeMillis();
        return new DbProperties(installTime,
                versionDetails.getVersionName(),
                versionDetails.getRevisionInt(),
                versionDetails.getTimestamp()
        );
    }

    @Override
    public boolean isConverting() {
        return conversionLock.isLockedByMe();
    }

    private void runConverters(List<ArtifactoryConverterAdapter> converters, CompoundVersionDetails fromVersion,
            CompoundVersionDetails toVersion) {
        for (ArtifactoryConverterAdapter converter : converters) {
            if (converter.isInterested(fromVersion, toVersion)) {
                converter.convert(fromVersion, toVersion);
            }
        }
    }

    private void assertIfNoOtherActiveServers() {
        if (!serviceConversionStarted) {
            serviceConversionStarted = true;
            ArtifactoryServersCommonService serversService = ContextHelper.get().beanForType(
                    ArtifactoryServersCommonService.class);
            if (serversService.getOtherActiveMembers().size() > 0) {
                conversionLock.release();
                throw new RuntimeException("Stopping Artifactory, couldn't start conversions, other active " +
                        "servers have been found");
            }
        }
    }

    private boolean isServiceConversionInterested() {
        return vp.getOriginalService() != null && !vp.getOriginalService().isCurrent();
    }

    private boolean isLocalHomeInterested() {
        for (ArtifactoryConverterAdapter converter : localHomeConverters) {
            if (converter.isInterested(vp.getOriginalHome(), vp.getRunning())) {
                return true;
            }
        }
        return false;
    }

    private boolean isHaInterested() {
        if (!artifactoryHome.isHaConfigured()) {
            return false;
        }
        for (ArtifactoryConverterAdapter converter : clusterHomeConverters) {
            if (converter.isInterested(vp.getOriginalHa(), vp.getRunning())) {
                return true;
            }
        }
        return false;
    }

    public List<ArtifactoryConverterAdapter> getLocalHomeConverters() {
        return localHomeConverters;
    }

    public List<ArtifactoryConverterAdapter> getClusterHomeConverters() {
        return clusterHomeConverters;
    }

    private class DummyLockFile implements LockFile {
        @Override
        public LockFile tryLock() {
            return this;
        }

        @Override
        public void release() {
        }

        @Override
        public boolean isLockedByMe() {
            return true;
        }
    }
}
