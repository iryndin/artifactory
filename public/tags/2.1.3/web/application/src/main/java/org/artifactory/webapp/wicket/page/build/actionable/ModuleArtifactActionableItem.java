package org.artifactory.webapp.wicket.page.build.actionable;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.build.api.Artifact;
import org.artifactory.webapp.actionable.RepoAwareActionableItemBase;
import org.artifactory.webapp.actionable.action.ShowInTreeAction;
import org.artifactory.webapp.wicket.util.ItemCssClass;

/**
 * The published module artifact actionable item
 *
 * @author Noam Y. Tenne
 */
public class ModuleArtifactActionableItem extends RepoAwareActionableItemBase {

    private Artifact artifact;

    /**
     * Main constructor
     *
     * @param repoPath Repo path of artifact
     * @param artifact Artifact object
     */
    public ModuleArtifactActionableItem(RepoPath repoPath, Artifact artifact) {
        super(repoPath);
        this.artifact = artifact;
    }

    public String getDisplayName() {
        return artifact.getName();
    }

    public String getCssClass() {
        return ItemCssClass.doc.getCssClass();
    }

    public void filterActions(AuthorizationService authService) {
        ShowInTreeAction action = new ShowInTreeAction();
        RepoPath repoPath = getRepoPath();
        if ((repoPath == null) || (!authService.canRead(repoPath))) {
            action.setEnabled(false);
        }
        getActions().add(action);
    }

    /**
     * Returns the artifact
     *
     * @return Artifact
     */
    public Artifact getArtifact() {
        return artifact;
    }
}