package org.artifactory.webapp.wicket.page.build;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.util.WicketUtils;

import static org.artifactory.webapp.wicket.page.build.BuildBrowserConstants.PATH_CONSTANTS;

/**
 * Displays the build browser breadcrumbs
 *
 * @author Noam Y. Tenne
 */
public class BuildBreadCrumbsPanel extends Panel {

    /**
     * Main constructor
     *
     * @param pageParameters Build browser page parameters
     */
    public BuildBreadCrumbsPanel(PageParameters pageParameters) {
        super("buildBreadCrumbs");
        setOutputMarkupId(true);

        add(new CssClass("bread-crumbs nice-bread-crumbs"));

        RepeatingView items = new RepeatingView("item");
        add(items);

        StringBuilder pathBuilder = new StringBuilder();

        pathBuilder.append(BuildBrowserConstants.BUILDS);
        items.add(new BreadCrumbItem(items.newChildId(), "All Builds", pathBuilder.toString(), true));

        for (String constant : PATH_CONSTANTS) {
            if (pageParameters.containsKey(constant)) {
                String constantValue = pageParameters.getString(constant);
                pathBuilder.append("/").append(constantValue);

                final String format = getString(constant + ".format");
                final String value = String.format(format, constantValue);
                items.add(new BreadCrumbItem(items.newChildId(), value, pathBuilder.toString(), false));
            }
        }
    }

    /**
     * Breadcrumb item object
     */
    private class BreadCrumbItem extends WebMarkupContainer {

        private BreadCrumbItem(String id, final String crumbTitle, final String crumbPath, boolean first) {
            super(id);

            final String appPath = WicketUtils.getWicketAppPath();
            add(new ExternalLink("link", appPath + crumbPath, crumbTitle));
            add(new WebMarkupContainer("sep").setVisible(!first));
        }
    }
}