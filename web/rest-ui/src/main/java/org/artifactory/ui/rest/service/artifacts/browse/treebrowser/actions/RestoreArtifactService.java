package org.artifactory.ui.rest.service.artifacts.browse.treebrowser.actions;

import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.trash.TrashService;
import org.artifactory.rest.common.model.artifact.BaseArtifact;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.action.RestoreArtifact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Restore an artifact from the trashcan to it's original repository
 *
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestoreArtifactService implements RestService {

    @Autowired
    TrashService trashService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        BaseArtifact artifact = (BaseArtifact) request.getImodel();
        RestoreArtifact restoreArtifact = (RestoreArtifact) request.getImodel();
        String repoKey = artifact.getRepoKey();
        String path = artifact.getPath();
        BasicStatusHolder status = trashService.restore(InternalRepoPathFactory.create(repoKey, path),
                restoreArtifact.getTargetRepoKey(), restoreArtifact.getTargetPath());
        if (status.isError()) {
            response.error(status.getLastError().getMessage());
        } else if (status.hasWarnings()) {
            response.warn(status.getLastWarning().getMessage());
        } else {
            response.info("Successfully restored artifact");
        }
    }
}
