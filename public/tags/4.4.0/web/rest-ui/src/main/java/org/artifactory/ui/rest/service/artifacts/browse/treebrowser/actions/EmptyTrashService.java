package org.artifactory.ui.rest.service.artifacts.browse.treebrowser.actions;

import org.artifactory.common.StatusHolder;
import org.artifactory.repo.trash.TrashService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class EmptyTrashService implements RestService {

    @Autowired
    TrashService trashService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        StatusHolder statusHolder = trashService.empty();
        if (statusHolder.isError()) {
            response.error(statusHolder.getLastError().getMessage());
        } else {
            response.info("Successfully delete all trashcan items");
        }
    }
}
