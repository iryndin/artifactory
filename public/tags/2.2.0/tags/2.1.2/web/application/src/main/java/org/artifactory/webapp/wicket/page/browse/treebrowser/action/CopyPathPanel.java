package org.artifactory.webapp.wicket.page.browse.treebrowser.action;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.common.StatusEntry;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.ajax.ConfirmationAjaxCallDecorator;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.webapp.wicket.page.browse.treebrowser.TreeBrowsePanel;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.List;

/**
 * Displays a list of repositories that are available to the user as a copy target
 *
 * @author Noam Y. Tenne
 */
public class CopyPathPanel extends MoveAndCopyBasePanel {

    @SpringBean
    private AuthorizationService authorizationService;

    @SpringBean
    private RepositoryService repoService;

    private RepoPath pathToCopy;
    private Component componentToRefresh;
    private TreeBrowsePanel browseRepoPanel;

    /**
     * Main constructor
     *
     * @param id                 Panel ID
     * @param pathToCopy         Path to copy
     * @param componentToRefresh Component to refresh after completing the copy
     * @param browseRepoPanel    An instance of the browse repo panel
     */
    public CopyPathPanel(String id, RepoPath pathToCopy, Component componentToRefresh,
            TreeBrowsePanel browseRepoPanel) {
        super(id);
        this.pathToCopy = pathToCopy;
        this.componentToRefresh = componentToRefresh;
        this.browseRepoPanel = browseRepoPanel;
        init();
    }

    protected TitledAjaxSubmitLink createSubmitButton(Form form, String wicketId) {
        return new TitledAjaxSubmitLink(wicketId, "Copy", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                String targetRepoKey = getSelectedTargetRepository();

                MoveMultiStatusHolder status = repoService.copy(pathToCopy, targetRepoKey, false);

                if (!status.isError() && !status.hasWarnings()) {
                    getPage().info("Successfully copied '" + pathToCopy + "'.");
                } else {
                    if (status.hasWarnings()) {
                        List<StatusEntry> warnings = status.getWarnings();
                        String logs;
                        if (authorizationService.isAdmin()) {
                            CharSequence systemLogsPage = WicketUtils.mountPathForPage(SystemLogsPage.class);
                            logs = "<a href=\"" + systemLogsPage + "\">log</a>";
                        } else {
                            logs = "log";
                        }
                        getPage().warn(warnings.size() + " warnings have been produced during the copy. Please " +
                                "review the " + logs + " for further information.");
                    }
                    if (status.isError()) {
                        String message = status.getStatusMsg();
                        Throwable exception = status.getException();
                        if (exception != null) {
                            message = exception.getMessage();
                        }
                        getPage().error("Failed to copy '" + pathToCopy + "': " + message);
                    }
                }

                //Colapse all tree nodes
                if (componentToRefresh instanceof Tree) {
                    //We collapse all since we don't know which path will eventually copy
                    Tree tree = (Tree) componentToRefresh;
                    ITreeState treeState = tree.getTreeState();
                    treeState.collapseAll();
                    DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModelObject();
                    treeState.selectNode(((TreeNode) treeModel.getRoot()).getChildAt(0), true);
                }

                browseRepoPanel.removeNodePanel(target);
                target.addComponent(componentToRefresh);
                AjaxUtils.refreshFeedback(target);
                ModalHandler.closeCurrent(target);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                //Add confirmation dialog when clicked
                String message = String.format("Are you sure you wish to copy '%s'?", pathToCopy);
                return new ConfirmationAjaxCallDecorator(message);
            }
        };
    }

    protected List<LocalRepoDescriptor> getDeployableLocalReposKeys() {
        return getDeployableLocalReposKeysExcludingSource(pathToCopy.getRepoKey());
    }

    protected MoveMultiStatusHolder executeDryRun(String targetRepoKey) {
        return repoService.copy(pathToCopy, targetRepoKey, true);
    }
}
