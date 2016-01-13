package org.artifactory.ui.rest.service.admin.security.sshserver;

import org.artifactory.api.security.SshAuthService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Noam Y. Tenne
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RemoveSshServerKeyService implements RestService {

    private static final Logger log = LoggerFactory.getLogger(RemoveSshServerKeyService.class);

    @Autowired
    SshAuthService sshAuthService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        boolean isPublic = Boolean.valueOf(request.getQueryParamByKey("public"));
        try {
            if (isPublic) {
                sshAuthService.removePublicKey();
            } else {
                sshAuthService.removePrivateKey();
            }
        } catch (Exception e) {
            log.error("Failed to remove key", e);
            response.error(e.toString());
            return;
        }
        response.info("Key was removed");
    }
}
