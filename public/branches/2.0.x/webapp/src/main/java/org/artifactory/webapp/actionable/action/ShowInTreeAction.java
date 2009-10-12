package org.artifactory.webapp.actionable.action;

import org.apache.wicket.RequestCycle;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;
import org.artifactory.webapp.wicket.page.browse.treebrowser.BrowseRepoPage;

/**
 * @author Yossi Shaul
 */
public class ShowInTreeAction extends RepoAwareItemAction {
    public static final String ACTION_NAME = "Show In Tree";

    public ShowInTreeAction() {
        super(ACTION_NAME, null);
    }

    @Override
    public void onAction(RepoAwareItemEvent e) {
        RepoAwareActionableItem source = e.getSource();
        RequestCycle.get().setResponsePage(new BrowseRepoPage(source));
    }
}