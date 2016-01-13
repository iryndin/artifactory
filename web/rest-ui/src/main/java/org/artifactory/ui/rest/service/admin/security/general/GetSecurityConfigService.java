package org.artifactory.ui.rest.service.admin.security.general;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.admin.security.general.SecurityConfig;
import org.artifactory.ui.rest.model.admin.security.login.UserLogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetSecurityConfigService implements RestService {

    @Autowired
    private CentralConfigService centralConfigService;

    @Autowired
    private SecurityService securityService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        UserLogin userLogin = (UserLogin) request.getImodel();

        MutableCentralConfigDescriptor centralConfig = centralConfigService.getMutableDescriptor();
        SecurityDescriptor securityDescriptor =  centralConfig.getSecurity();
        SecurityConfig securityConfig = new SecurityConfig(securityDescriptor.isAnonAccessEnabled(),
                securityDescriptor.isAnonAccessToBuildInfosDisabled()
                ,securityDescriptor.isHideUnauthorizedResources(),
                securityDescriptor.getPasswordSettings(),
                securityDescriptor.getUserLockPolicy());

        // set number of days left till password expiers
        if(userLogin != null) {
            Integer userPasswordDaysLeft = securityService.getUserPasswordDaysLeft(userLogin.getUser());
            if (userPasswordDaysLeft != null) {
                securityConfig.getPasswordSettings().getExpirationPolicy().setCurrentPasswordValidFor(userPasswordDaysLeft);
            }
        }

        response.iModel(securityConfig);
    }
}
