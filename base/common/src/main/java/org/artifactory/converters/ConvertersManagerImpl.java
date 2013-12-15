package org.artifactory.converters;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.property.ArtifactoryConverter;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.state.model.ArtifactoryStateManager;
import org.artifactory.storage.db.servers.service.ArtifactoryServersCommonService;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.CompoundVersionDetails;
import org.artifactory.version.ConfigVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.artifactory.logging.version.LoggingVersion.LOGGING_CONVERSION_PERFORMED;


/**
 * Author: gidis
 * The class manages the conversions process during Artifactory life cycle.
 * It creates a complete separation between the HOME, CLUSTER and database
 * environments, thoughts Artifactory converts each environment independently,
 * each environment has its own original version and each original version might
 * trigger the relevant conversion.
 */
public class ConvertersManagerImpl implements ConverterProvider, VersionProvider {
    private static final Logger log = LoggerFactory.getLogger(ConvertersManagerImpl.class);

    private final VersionProviderImpl vm;
    private final HomeConverterHandler homeConverterHandler;
    private final HaConverterHandler haConverterHandler;
    private final ServiceConverterHandler serviceConverterHandler;
    private ArtifactoryHome artifactoryHome;
    private boolean conversionPerformed;
    private ConversionLock conversionLock;

    public ConvertersManagerImpl(ArtifactoryHome artifactoryHome) {
        this.artifactoryHome = artifactoryHome;
        vm = new VersionProviderImpl(artifactoryHome);
        // Verify that the db version is supported for conversions
        CompoundVersionDetails running = vm.getRunningVersionDetails();
        CompoundVersionDetails originalHome = vm.getOriginalHomeVersionDetails();
        CompoundVersionDetails originalHa = vm.getOriginalHaVersionDetails();
        verifySupportedVersion(originalHome.getVersion(), running.getVersion());
        verifySupportedVersion(originalHa.getVersion(), running.getVersion());
        homeConverterHandler = new HomeConverterHandler(vm, artifactoryHome);
        haConverterHandler = new HaConverterHandler(vm, artifactoryHome);
        serviceConverterHandler = new ServiceConverterHandler(vm);
        conversionLock = new ConversionLock(artifactoryHome, vm);
    }


    @Override
    public void homeReady() {
        ArtifactorySystemProperties properties = artifactoryHome.getArtifactoryProperties();
        conversionPerformed = Boolean.valueOf(properties.getProperty(LOGGING_CONVERSION_PERFORMED, "false"));
        if (!conversionPerformed) {
            CompoundVersionDetails running = vm.getRunningVersionDetails();
            CompoundVersionDetails originalHome = vm.getOriginalHomeVersionDetails();
            CompoundVersionDetails originalHa = vm.getOriginalHaVersionDetails();
            if (isHomeInterested()) {
                conversionLock.lock();
                homeConverterHandler.convert(originalHome, running);
                log.info("Home conversion started");
            }
            if (isHaInterested()) {
                conversionLock.lock();
                haConverterHandler.convert(originalHa, running);
                log.info("Cluster home conversion started");
            }
            // At this stage the DBService is not ready therefore, check if the service converter is interested during the dbAccessReady event
        }
    }

    @Override
    public void dbAccessReady() {
        if (!conversionPerformed) {
            vm.dbReady();
            // Verify that the db version is supported for conversions
            CompoundVersionDetails serviceVersion = vm.getOriginalServiceVersionDetails();
            CompoundVersionDetails runningVersion = vm.getRunningVersionDetails();
            verifySupportedVersion(serviceVersion.getVersion(), runningVersion.getVersion());
            // Only now we can check if the service converter is interested
            if (isServiceInterested()) {
                conversionLock.lock();
                log.info("DB & Services conversion started");
            }
            if (conversionLock.isMyLock()) {
                // StopConversion if other servers are active
                ArtifactoryServersCommonService serversService = ContextHelper.get().beanForType(
                        ArtifactoryServersCommonService.class);
                try {
                    if (serversService.getOtherActiveMembers().size() > 0) {
                        throw new RuntimeException("Stopping Artifactory, couldn't start conversions, other active " +
                                "servers have been found");
                    }
                } catch (Exception e) {
                    // We can assume that the table doesn't exists and no other servers are connected to the DB
                }
            }
        }
    }

    @Override
    public void serviceConvert(ArtifactoryConverter artifactoryConverter) {
        if (!conversionPerformed) {
            if (isServiceInterested()) {
                if (serviceConverterHandler.isInterested(vm.getOriginalServiceVersionDetails(),
                        vm.getRunningVersionDetails())) {
                    serviceConverterHandler.convert(artifactoryConverter);
                }
            }
        }
    }

    @Override
    public void conversionEnded() {
        if (!conversionPerformed) {
            ArtifactorySystemProperties properties = artifactoryHome.getArtifactoryProperties();
            properties.setProperty(LOGGING_CONVERSION_PERFORMED, "true");
            if (conversionLock.isMyLock()) {
                try {
                    // Update Service version;
                    CompoundVersionDetails runningVersion = vm.getRunningVersionDetails();
                    if (serviceConverterHandler.isInterested(vm.getOriginalServiceVersionDetails(), runningVersion)) {
                        serviceConverterHandler.conversionEnded();
                    }
                    // Update home version
                    if (homeConverterHandler.isInterested(vm.getOriginalHomeVersionDetails(), runningVersion)) {
                        homeConverterHandler.conversionEnded();
                    }
                    // Update ha version
                    if (haConverterHandler.isInterested(vm.getOriginalHaVersionDetails(), runningVersion)) {
                        haConverterHandler.conversionEnded();
                    }
                    ArtifactoryStateManager stateManager = ContextHelper.get().beanForType(
                            ArtifactoryStateManager.class);
                    stateManager.conversionFinished();
                } finally {
                    log.info("All conversions ended");
                    conversionLock.unlock();
                }
            }
        }
    }

    @Override
    public boolean isConverting() {
        return conversionLock.isMyLock();
    }

    @Override
    public CompoundVersionDetails getRunningVersionDetails() {
        return vm.getRunningVersionDetails();
    }

    @Override
    public CompoundVersionDetails getOriginalHaVersionDetails() {
        return vm.getOriginalHaVersionDetails();
    }

    @Override
    public CompoundVersionDetails getOriginalHomeVersionDetails() {
        return vm.getOriginalHomeVersionDetails();
    }

    @Override
    public CompoundVersionDetails getOriginalServiceVersionDetails() {
        return vm.getOriginalServiceVersionDetails();
    }


    private boolean isHomeInterested() {
        CompoundVersionDetails running = vm.getRunningVersionDetails();
        CompoundVersionDetails originalHome = vm.getOriginalHomeVersionDetails();
        boolean homeInterested = homeConverterHandler.isInterested(originalHome, running);
        return homeInterested;
    }

    private boolean isHaInterested() {
        CompoundVersionDetails running = vm.getRunningVersionDetails();
        CompoundVersionDetails originalHa = vm.getOriginalHaVersionDetails();
        boolean haInterested = haConverterHandler.isInterested(originalHa, running);
        return haInterested;
    }

    private boolean isServiceInterested() {
        CompoundVersionDetails running = vm.getRunningVersionDetails();
        CompoundVersionDetails originalService = vm.getOriginalServiceVersionDetails();
        boolean serviceInterest = serviceConverterHandler.isInterested(originalService, running);
        return serviceInterest;
    }

    public void verifySupportedVersion(ArtifactoryVersion original, ArtifactoryVersion running) {
        if (!vm.getRunningVersionDetails().getVersion().equals(original)) {
            // the version written in the jar and the version read from the data directory are different
            // make sure the version from the data directory is supported by the current deployed artifactory
            ConfigVersion actualConfigVersion = ConfigVersion.findCompatibleVersion(original);
            //No compatible version -> conversion needed, but supported only from v4 onward
            if (!actualConfigVersion.isCurrent()) {
                String msg = "The stored version for (" + original.getValue() + ") " +
                        "is not up-to-date with the currently deployed Artifactory (" +
                        running + ")";
                if (!actualConfigVersion.isAutoUpdateCapable()) {
                    //Cannot convert
                    msg += ": no automatic conversion is possible. Exiting now...";
                    throw new IllegalStateException(msg);
                }
            }
        }
    }
}
