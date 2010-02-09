package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.action;

import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.event.ItemEvent;

/**
 * Redirects to the build's CI server URL
 *
 * @author Noam Y. Tenne
 */
public class ShowInCiServerAction extends ItemAction {

    private static String ACTION_NAME = "Show in CI Server";
    private String buildUrl;

    /**
     * Main constructor
     *
     * @param buildUrl CI server build URL
     */
    public ShowInCiServerAction(String buildUrl) {
        super(ACTION_NAME);
        this.buildUrl = buildUrl;
    }

    @Override
    public String getActionLinkURL(ActionableItem actionableItem) {
        return buildUrl;
    }

    @Override
    public void onAction(ItemEvent e) {
        // this method should not be called for this action
    }

}