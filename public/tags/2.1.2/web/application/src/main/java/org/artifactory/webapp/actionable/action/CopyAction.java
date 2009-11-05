package org.artifactory.webapp.actionable.action;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.common.wicket.component.modal.panel.bordered.BorderedModalPanel;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.event.ItemEventTargetComponents;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;
import org.artifactory.webapp.wicket.page.browse.treebrowser.TreeBrowsePanel;
import org.artifactory.webapp.wicket.page.browse.treebrowser.action.CopyPathPanel;

/**
 * Enables the user to copy the currently selected location of the tree to another repository
 *
 * @author Noam Y. Tenne
 */
public class CopyAction extends RepoAwareItemAction {

    public static final String ACTION_NAME = "Copy";

    /**
     * Default constructor
     */
    public CopyAction() {
        super(ACTION_NAME);
    }

    @Override
    public String getDisplayName(ActionableItem actionableItem) {
        return "Copy...";
    }

    public void onAction(RepoAwareItemEvent event) {
        RepoPath repoPath = event.getRepoPath();

        //Create a modal window and add the move path panel to it
        ItemEventTargetComponents eventTargetComponents = event.getTargetComponents();
        //Should be the tree
        Component tree = eventTargetComponents.getRefreshableComponent();

        WebMarkupContainer nodaPanelContainer = eventTargetComponents.getNodePanelContainer();
        TreeBrowsePanel browseRepoPanel = (TreeBrowsePanel) nodaPanelContainer.getParent();

        ModalWindow modalWindow = eventTargetComponents.getModalWindow();
        CopyPathPanel panel = new CopyPathPanel(modalWindow.getContentId(), repoPath, tree, browseRepoPanel);

        BorderedModalPanel modalPanel = new BorderedModalPanel(panel);
        modalPanel.setWidth(500);
        modalPanel.setTitle(String.format("Copy '%s'", repoPath));
        modalWindow.setContent(modalPanel);
        modalWindow.show(event.getTarget());
    }
}
