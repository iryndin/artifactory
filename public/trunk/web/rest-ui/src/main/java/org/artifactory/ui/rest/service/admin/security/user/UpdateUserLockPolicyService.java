package org.artifactory.ui.rest.service.admin.security.user;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.security.UserLockPolicy;
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
public class UpdateUserLockPolicyService<T extends UserLockPolicy> implements RestService<T> {

    @Autowired
    private CentralConfigService centralConfigService;

    @Override
    public void execute(ArtifactoryRestRequest<T> request, RestResponse response) {
        UserLockPolicy userLockPolicy = request.getImodel();

        if (userLockPolicy.getLoginAttempts() > 100 || userLockPolicy.getLoginAttempts()< 1) {
            response.responseCode(400);
            response.error("LoginAttempts must be between 1 - 100");
            return;
        }

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
