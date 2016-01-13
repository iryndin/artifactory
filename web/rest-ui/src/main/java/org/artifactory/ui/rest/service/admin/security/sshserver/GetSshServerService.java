package org.artifactory.ui.rest.service.admin.security.sshserver;

import com.google.common.base.Joiner;
import org.apache.ivy.plugins.repository.ssh.SshResource;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.SshAuthService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.security.sshserver.SshServerSettings;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.rest.common.util.AolUtils;
import org.artifactory.ui.rest.model.admin.configuration.generalconfig.GeneralConfig;
import org.artifactory.ui.rest.model.admin.security.signingkey.SignKey;
import org.artifactory.ui.rest.model.admin.security.sshserver.SshServer;
import org.artifactory.util.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Noam Y. Tenne
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetSshServerService implements RestService {

    @Autowired
    private CentralConfigService centralConfigService;

    @Autowired
    private SshAuthService sshAuthService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        AolUtils.assertNotAol("GetSshServer");
        SecurityDescriptor securityDescriptor = getSecurityDescriptor();
        SshServer sshServer = getSshServer(request, securityDescriptor);
        response.iModel(sshServer);
    }

    private SshServer getSshServer(ArtifactoryRestRequest request, SecurityDescriptor securityDescriptor) {
        SshServerSettings sshServerSettings = securityDescriptor.getSshServerSettings();

        MutableCentralConfigDescriptor mutableDescriptor = centralConfigService.getMutableDescriptor();
        GeneralConfig generalConfig = new GeneralConfig(mutableDescriptor);

        if (sshServerSettings == null) {
            SshServer sshServer = new SshServer();
            sshServer.setEnableSshServer(false);
            sshServer.setSshServerPort(1339);
            sshServer.setCustomUrlBase(generalConfig.getCustomUrlBase());
            return sshServer;
        }
        SshServer sshServer = new SshServer(sshServerSettings);
        sshServer.setCustomUrlBase(generalConfig.getCustomUrlBase());
        sshServer.setServerKey(serverKeys(request));
        return sshServer;
    }

    private SecurityDescriptor getSecurityDescriptor() {
        MutableCentralConfigDescriptor centralConfig = centralConfigService.getMutableDescriptor();
        return centralConfig.getSecurity();
    }

    private SignKey serverKeys(ArtifactoryRestRequest request) {
        SignKey signKey = new SignKey();
        signKey.setPrivateKeyInstalled(sshAuthService.hasPrivateKey());
        boolean publicKeyInstalled = sshAuthService.hasPublicKey();
        if (publicKeyInstalled) {
            String link = getKeyLink(request.getServletRequest());
            signKey.setPublicKeyInstalled(true);
            signKey.setPublicKeyLink(link);
        }
        return signKey;
    }

    private String getKeyLink(HttpServletRequest request) {
        return Joiner.on('/').join(HttpUtils.getServletContextUrl(request),
                "api", "ssh", "key/public");
    }
}
