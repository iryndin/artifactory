package org.artifactory.ui.rest.service.artifacts.browse.treebrowser.tabs.docker;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.docker.DockerAddon;
import org.artifactory.addon.docker.DockerV2InfoModel;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.BaseArtifactInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DockerV2ViewService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(DockerV2ViewService.class);

    @Autowired
    RepositoryService repoService;

    @Autowired
    CentralConfigService configService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        BaseArtifactInfo artifactInfo = (BaseArtifactInfo) request.getImodel();
        String path = artifactInfo.getPath();
        String repoKey = artifactInfo.getRepoKey();
        RepoPath repoPath = InternalRepoPathFactory.create(repoKey, path);
        ItemInfo manifest = repoService.getChildren(repoPath)
                .stream().filter(itemInfo -> itemInfo.getName().equals("manifest.json"))
                .findFirst().orElseGet(null);
        if (manifest != null) {
            try{
                DockerV2InfoModel dockerV2Info = ContextHelper.get().beanForType(AddonsManager.class)
                        .addonByType(DockerAddon.class).getDockerV2Model(manifest.getRepoPath());
                response.iModel(dockerV2Info);
            } catch (Exception e) {
                String err = "Unable to extract Docker metadata for '" + repoPath;
                response.error(err);
                log.error(err);
                log.debug(err, e);
            }
        } else {
            response.error("Unable to find Docker manifest under '" + repoPath);
            log.error("Unable to find Docker manifest under '" + repoPath + "'");
        }
    }
}
