package org.artifactory.webapp.wicket.page.build.tabs.list;

import com.google.common.collect.Lists;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.build.api.Artifact;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.page.build.actionable.ModuleArtifactActionableItem;
import org.slf4j.Logger;

import java.util.List;

/**
 * The disabled module artifacts list panel
 *
 * @author Noam Y. Tenne
 */
public class DisabledModuleArtifactsListPanel extends BaseModuleArtifactsListPanel {

    private static final Logger log = LoggerFactory.getLogger(DisabledModuleArtifactsListPanel.class);

    /**
     * Main constructor
     *
     * @param id ID to assign to the panel
     */
    public DisabledModuleArtifactsListPanel(String id) {
        super(id);

        try {
            addTable();
        } catch (RepositoryRuntimeException rre) {
            String errorMessage = "An error occurred while loading the produced artifact list";
            log.error(errorMessage, rre);
            error(errorMessage);
        }
    }

    @Override
    protected List<Artifact> getArtifacts() {
        return Lists.newArrayList();
    }

    @Override
    protected List<ModuleArtifactActionableItem> getModuleArtifactActionableItems(List<Artifact> artifacts) {
        return Lists.newArrayList();
    }
}