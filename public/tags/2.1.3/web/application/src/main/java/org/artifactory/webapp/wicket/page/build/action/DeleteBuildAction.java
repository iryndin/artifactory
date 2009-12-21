package org.artifactory.webapp.wicket.page.build.action;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.target.basic.RedirectRequestTarget;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.search.SearchService;
import org.artifactory.build.api.Build;
import org.artifactory.build.api.BuildService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.actionable.action.DeleteAction;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.event.ItemEvent;
import static org.artifactory.webapp.wicket.page.build.BuildBrowserConstants.BUILDS;
import org.artifactory.webapp.wicket.page.build.BuildBrowserRootPage;
import org.slf4j.Logger;

import java.util.List;

/**
 * Deletes the selected build
 *
 * @author Noam Y. Tenne
 */
public class DeleteBuildAction extends ItemAction {

    private static final Logger log = LoggerFactory.getLogger(DeleteBuildAction.class);

    private static String ACTION_NAME = "Delete";
    private Build build;

    /**
     * Main constructor
     *
     * @param build Build to delete
     */
    public DeleteBuildAction(Build build) {
        super(ACTION_NAME);
        this.build = build;
    }

    @Override
    public void onAction(ItemEvent e) {
        AjaxRequestTarget target = e.getTarget();
        BuildBrowserRootPage rootPage = (BuildBrowserRootPage) target.getPage();

        BuildService buildService = ContextHelper.get().beanForType(BuildService.class);
        String buildName = build.getName();
        long buildNumber = build.getNumber();

        try {
            buildService.deleteBuild(build);
        } catch (Exception exception) {
            String error = String.format("Exception occurred while deleting build '%s' #%s", buildName, buildNumber);
            log.error(error, e);
            rootPage.error(error);
            return;
        }

        SearchService searchService = ContextHelper.get().beanForType(SearchService.class);
        List<Build> remainingBuilds = searchService.searchBuildsByName(buildName);

        if (remainingBuilds.isEmpty()) {
            RequestCycle.get().setRequestTarget(new RedirectRequestTarget(BUILDS));
        } else {
            String buildUrl = new StringBuilder().append(BUILDS).append("/").append(buildName).toString();
            RequestCycle.get().setRequestTarget(new RedirectRequestTarget(buildUrl));
        }
    }

    @Override
    public String getCssClass() {
        return DeleteAction.class.getSimpleName();
    }
}