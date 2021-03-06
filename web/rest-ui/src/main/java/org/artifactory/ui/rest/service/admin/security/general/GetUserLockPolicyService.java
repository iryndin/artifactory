package org.artifactory.ui.rest.service.admin.security.general;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Michael Pasternak
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetUserLockPolicyService implements RestService {

    @Autowired
    private CentralConfigService centralConfigService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        response.iModel(centralConfigService.getDescriptor().getSecurity().getUserLockPolicy());
    }
}
