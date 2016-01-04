package org.artifactory.ui.rest.service.admin.security.user;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.security.UserLockPolicy;
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
 * @author Michael Pasternak
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateUserLockPolicyService<T extends UserLockPolicy> implements RestService<T> {

    @Autowired
    private CentralConfigService centralConfigService;

    @Override
    public void execute(ArtifactoryRestRequest<T> request, RestResponse response) {
        UserLockPolicy userLockPolicy = request.getImodel();

        UserLockPolicy userLockPolicyConfig =
                centralConfigService.getDescriptor().getSecurity().getUserLockPolicy();
        if (userLockPolicyConfig == null) {
            centralConfigService.getDescriptor().getSecurity().setUserLockPolicy(userLockPolicy);
        } else {
            userLockPolicyConfig.setEnabled(userLockPolicy.isEnabled());
            userLockPolicyConfig.setLoginAttempts(userLockPolicy.getLoginAttempts());
        }
        centralConfigService.saveEditedDescriptorAndReload(centralConfigService.getDescriptor());

        response.info("UserLockPolicy was successfully updated");
    }
}
