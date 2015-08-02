package org.artifactory.ui.rest.service.artifacts.deploy;

import org.artifactory.api.artifact.ArtifactInfo;
import org.artifactory.api.artifact.UnitInfo;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.DeployService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.request.ArtifactoryRequestBase;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.md.Properties;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.deploy.UploadArtifactInfo;
import org.artifactory.ui.rest.model.artifacts.deploy.UploadedArtifactInfo;
import org.artifactory.ui.utils.MultiPartUtils;
import org.artifactory.ui.utils.TreeUtils;
import org.artifactory.util.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ArtifactMultiDeployService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(ArtifactMultiDeployService.class);

    @Autowired
    DeployService deployService;

    @Autowired
    CentralConfigService centralConfigService;

    @Autowired
    RepositoryService repositoryService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        String repoKey = request.getQueryParamByKey("repoKey");
        String path = request.getQueryParamByKey("path");
        List<String> fileNames = saveFileToTempFolder(request, response);

        if (!fileNames.isEmpty()) {
            //deploy file
            deploy(fileNames.get(0), response, repoKey, path);
        }
    }

    /**
     * upload file to temp folder
     *
     * @param artifactoryRequest - encapsulate data related to request
     * @param response           - encapsulate data require for response
     * @return - true if uploaded successful
     */
    private List<String> saveFileToTempFolder(ArtifactoryRestRequest artifactoryRequest, RestResponse response) {
        String uploadDir = ContextHelper.get().getArtifactoryHome().getTempUploadDir().getAbsolutePath();
        List<String> fileNames = new ArrayList<>();
        try {
            MultiPartUtils.createTempFolderIfNotExist(uploadDir);
            // get upload model
            UploadArtifactInfo uploadArtifactInfo = (UploadArtifactInfo) artifactoryRequest.getImodel();
            // save file data tto temp
            MultiPartUtils.saveFileDataToTemp(centralConfigService, uploadArtifactInfo.fetchFormDataMultiPart(), uploadDir,
                    fileNames, false);
        } catch (Exception e) {
            response.error(e.getMessage());
        }
        return fileNames;
    }

    /**
     * deploy file to repository
     *
     * @param artifactoryResponse - encapsulate data require for response
     */
    private void deploy(String fileName, RestResponse artifactoryResponse, String repoKey, String path) {
        String uploadDir = ContextHelper.get().getArtifactoryHome().getTempUploadDir().getAbsolutePath();
        Properties properties = parseMatrixParams(fileName);
        LocalRepoDescriptor localRepoDescriptor = repositoryService.localOrCachedRepoDescriptorByKey(repoKey);
        try {
            File file = new File(uploadDir, fileName);
            UnitInfo unitInfo = new ArtifactInfo(path);
            // deploy file
            deployService.deploy(localRepoDescriptor, unitInfo, file, properties);
            // delete tmp file
            Files.removeFile(file);
            UploadedArtifactInfo uploadedArtifactInfo = new UploadedArtifactInfo(TreeUtils.shouldProvideTreeLink(localRepoDescriptor, unitInfo.getPath()), repoKey, unitInfo.getPath());
            artifactoryResponse.iModel(uploadedArtifactInfo);
        } catch (RepoRejectException e) {
            log.error(e.toString());
            artifactoryResponse.error("failed to deploy file:" + fileName + " to Repository: " + repoKey);
        }
    }

    /**
     * get uploaded file Properties
     *
     * @param fileName - file name
     * @return
     */
    private Properties parseMatrixParams(String fileName) {
        Properties props;
        props = (Properties) InfoFactoryHolder.get().createProperties();
        String targetPathFieldValue = fileName;
        int matrixParamStart = targetPathFieldValue.indexOf(Properties.MATRIX_PARAMS_SEP);
        if (matrixParamStart > 0) {
            ArtifactoryRequestBase.processMatrixParams(props, targetPathFieldValue.substring(matrixParamStart));
        }
        return props;
    }
}
