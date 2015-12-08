package org.artifactory.ui.rest.service.admin.advanced;

import org.artifactory.support.config.bundle.BundleConfiguration;
import org.artifactory.ui.rest.service.admin.advanced.configDescriptor.GetConfigDescriptorService;
import org.artifactory.ui.rest.service.admin.advanced.configDescriptor.UpdateConfigDescriptorService;
import org.artifactory.ui.rest.service.admin.advanced.maintenance.*;
import org.artifactory.ui.rest.service.admin.advanced.securitydescriptor.GetSecurityDescriptorService;
import org.artifactory.ui.rest.service.admin.advanced.securitydescriptor.UpdateSecurityDescriptorService;
import org.artifactory.rest.common.service.admin.advance.GetStorageSummaryService;
import org.artifactory.ui.rest.service.admin.advanced.support.BundleConfigurationWrapper;
import org.artifactory.ui.rest.service.admin.advanced.support.SupportServiceDeleteBundle;
import org.artifactory.ui.rest.service.admin.advanced.support.SupportServiceGenerateBundle;
import org.artifactory.ui.rest.service.admin.advanced.support.SupportServiceDownloadBundle;
import org.artifactory.ui.rest.service.admin.advanced.support.SupportServiceListBundles;
import org.artifactory.ui.rest.service.admin.advanced.systeminfo.GetSystemInfoService;
import org.artifactory.ui.rest.service.admin.advanced.systemlogs.GetSysLogDownloadLinkService;
import org.artifactory.ui.rest.service.admin.advanced.systemlogs.GetSysLogsInitializeService;
import org.artifactory.ui.rest.service.admin.advanced.systemlogs.GetSysLogDataService;
import org.springframework.beans.factory.annotation.Lookup;

/**
 * @author Chen Keinan
 */
public abstract class AdvancedServiceFactory {

    // storage summary service
    @Lookup
    public abstract GetStorageSummaryService getStorageSummaryService();
    // system info service
    @Lookup
    public abstract GetSystemInfoService getSystemInfoService();
    // config descriptor service
    @Lookup
    public abstract UpdateConfigDescriptorService updateConfigDescriptorService();

    @Lookup
    public abstract GetConfigDescriptorService getConfigDescriptorService();
    // security descriptor service
    @Lookup
    public abstract UpdateSecurityDescriptorService updateSecurityConfigService();

    @Lookup
    public abstract GetSecurityDescriptorService getSecurityDescriptorService();

    @Lookup
    public abstract CleanUnusedCachedService cleanUnusedCached();

    @Lookup
    public abstract CleanupVirtualRepoService cleanupVirtualRepo();

    @Lookup
    public abstract GarbageCollectionService garbageCollection();

    @Lookup
    public abstract SaveMaintenanceService saveMaintenance();

    @Lookup
    public abstract PruneUnReferenceDataService pruneUnReferenceData();

    @Lookup
    public abstract CompressInternalDataService compressInternalData();

    @Lookup
    public abstract GetMaintenanceService getMaintenance();

    @Lookup
    public abstract GetSysLogDataService getSystemLogData();

    @Lookup
    public abstract GetSysLogsInitializeService getSystemLogsInitialize();

    @Lookup
    public abstract GetSysLogDownloadLinkService getSystemLogDownloadLink();

    @Lookup
    public abstract SupportServiceGenerateBundle<BundleConfigurationWrapper> getSupportServiceGenerateBundle();

    @Lookup
    public abstract SupportServiceDownloadBundle<String> getSupportServiceDownloadBundle();

    @Lookup
    public abstract SupportServiceListBundles getSupportServiceListBundles();

    @Lookup
    public abstract SupportServiceDeleteBundle getSupportServiceDeleteBundle();
}
