package org.artifactory.ui.rest.service.admin.security.sshserver;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.SshAuthService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Noam Y. Tenne
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetSshServerKeyService implements RestService {

    @Autowired
    CentralConfigService centralConfigService;

    @Autowired
    SshAuthService sshAuthService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
    }
}
