package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.action;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.request.target.basic.RedirectRequestTarget;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.event.ItemEvent;
import org.artifactory.webapp.wicket.page.build.BuildBrowserConstants;

/**
 * Redirects to the build history view of the given build name
 *
 * @author Noam Y. Tenne
 */
public class GoToBuildAction extends ItemAction {

    private static String ACTION_NAME = "Go To Build";
    private String buildName;
    private long buildNumber;

    /**
     * Main constructor
     *
     * @param buildName   Name of build to go to
     * @param buildNumber Number of build to go to
     */
    public GoToBuildAction(String buildName, long buildNumber) {
        super(ACTION_NAME);
        this.buildName = buildName;
        this.buildNumber = buildNumber;
    }

    @Override
    public void onAction(ItemEvent e) {
        String url = new StringBuilder().append(BuildBrowserConstants.BUILDS).append("/").append(buildName).append("/").
                append(buildNumber).toString();
        RequestCycle.get().setRequestTarget(new RedirectRequestTarget(url));
    }
}