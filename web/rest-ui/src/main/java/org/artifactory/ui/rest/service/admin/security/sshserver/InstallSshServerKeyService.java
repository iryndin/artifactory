package org.artifactory.ui.rest.service.admin.security.sshserver;

import com.google.common.base.Joiner;
import org.apache.commons.io.FileUtils;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.SshAuthService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.admin.security.signingkey.SignKey;
import org.artifactory.ui.rest.model.utils.FileUpload;
import org.artifactory.ui.utils.MultiPartUtils;
import org.artifactory.util.Files;
import org.artifactory.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Noam Y. Tenne
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class InstallSshServerKeyService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(InstallSshServerKeyService.class);

    @Autowired
    CentralConfigService centralConfigService;

    @Autowired
    SshAuthService sshAuthService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        boolean isPublic = Boolean.valueOf(request.getQueryParamByKey("public"));
        List<String> fileNames = new ArrayList<>();
        saveKeyToTempFolder(request, fileNames, response);
        installKey(response, isPublic, fileNames, request.getServletRequest());
    }

    private void saveKeyToTempFolder(ArtifactoryRestRequest artifactoryRequest, List<String> fileNames,
            RestResponse response) {
        FileUpload uploadFile = (FileUpload) artifactoryRequest.getImodel();
        String uploadDir = ContextHelper.get().getArtifactoryHome().getTempUploadDir().getAbsolutePath();
        MultiPartUtils.createTempFolderIfNotExist(uploadDir);
        try {
            MultiPartUtils.saveFileDataToTemp(centralConfigService, uploadFile.getFormDataMultiPart(), uploadDir,
                    fileNames, false);
        } catch (Exception e) {
            response.error(e.getMessage());
        }
    }

    private void installKey(RestResponse artifactoryResponse, boolean isPublic, List<String> fileNames,
            HttpServletRequest request) {
        String uploadDir = ContextHelper.get().getArtifactoryHome().getTempUploadDir().getAbsolutePath();
        File file = new File(uploadDir, fileNames.get(0));
        try {
            String key = FileUtils.readFileToString(file);
            if (isPublic) {
                sshAuthService.savePublicKey(key);
                artifactoryResponse.info("Public key is installed");
            } else {
                sshAuthService.savePrivateKey(key);
                artifactoryResponse.info("Private key is installed");
            }
            String publicKeyDownloadTarget = getKeyLink(request);
            SignKey signKey = new SignKey(publicKeyDownloadTarget);
            Files.removeFile(file);
            artifactoryResponse.iModel(signKey);

            // delete temp key
            Files.removeFile(file);
        } catch (Exception e) {
            log.error(e.toString());
            artifactoryResponse.error(e.toString());
        }
    }

    private String getKeyLink(HttpServletRequest request) {
        return Joiner.on('/').join(HttpUtils.getServletContextUrl(request),
                "api", "ssh", "key/public");
    }
}
