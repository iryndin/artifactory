package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.actionable;

import org.artifactory.api.build.BasicBuildInfo;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.repo.RepoPath;

/**
 * @author Yoav Aharoni
 */
public class BuildDependencyActionableItem extends BuildTabActionableItem {
    private String scope;

    public BuildDependencyActionableItem(ModalHandler textContentViewer, BasicBuildInfo basicBuildInfo, String moduleId,
            String scope, RepoPath repoPath) {
        super(textContentViewer, basicBuildInfo, moduleId, repoPath);
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }
}