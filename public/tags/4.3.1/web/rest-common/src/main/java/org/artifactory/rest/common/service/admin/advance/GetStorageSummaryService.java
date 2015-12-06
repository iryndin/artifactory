package org.artifactory.rest.common.service.admin.advance;

import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.storage.StorageService;
import org.artifactory.storage.StorageSummaryInfo;
import org.artifactory.rest.common.model.storagesummary.StorageSummaryModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetStorageSummaryService implements RestService {

    @Autowired
    private StorageService storageService;


    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        StorageSummaryInfo storageSummaryInfo = storageService.getStorageSummaryInfo();
        StorageSummaryModel storageSummaryModel = new StorageSummaryModel(storageSummaryInfo);
        response.iModel(storageSummaryModel);
    }
}
