package org.artifactory.ui.rest.service.artifacts.deploy;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.deploy.UploadArtifactInfo;
import org.artifactory.util.Files;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CancelArtifactUpload implements RestService {

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        String uploadDir = ContextHelper.get().getArtifactoryHome().getTempUploadDir().getAbsolutePath();
        String fileName = ((UploadArtifactInfo) request.getImodel()).getFileName();
        File file = new File(uploadDir, fileName);
        // remove artifact  from temp folder
        Files.removeFile(file);
    }
}
