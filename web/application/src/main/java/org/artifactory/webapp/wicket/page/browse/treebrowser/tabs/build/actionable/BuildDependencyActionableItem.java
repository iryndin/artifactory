package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.actionable;

import org.artifactory.api.build.BasicBuildInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.action.ShowBuildItemInTreeAction;

/**
 * @author Yoav Aharoni
 */
public class BuildDependencyActionableItem extends BuildTabActionableItem {
    private String scope;

    public BuildDependencyActionableItem(ModalHandler textContentViewer, BasicBuildInfo basicBuildInfo, String moduleId,
            String scope, RepoPath repoPath) {
        super(textContentViewer, basicBuildInfo, moduleId);
        this.scope = scope;

        if (repoPath != null) {
            getActions().add(new ShowBuildItemInTreeAction(repoPath));
        }
    }

    public String getScope() {
        return scope;
    }
}