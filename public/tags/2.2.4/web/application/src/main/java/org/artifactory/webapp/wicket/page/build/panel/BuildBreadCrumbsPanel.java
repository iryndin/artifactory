/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.webapp.wicket.page.build.panel;

import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.webapp.wicket.page.build.page.BuildBrowserRootPage;

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

        items.add(new BreadCrumbItem(items.newChildId(), "All Builds", new PageParameters(), true));

        PageParameters responseParams = new PageParameters();
        for (String constant : PATH_CONSTANTS) {
            if (pageParameters.containsKey(constant)) {
                String constantValue = pageParameters.getString(constant);
                responseParams.put(constant, constantValue);

                final String format = getString(constant + ".format");
                final String value = String.format(format, constantValue);
                items.add(new BreadCrumbItem(items.newChildId(), value, new PageParameters(responseParams), false));
            }
        }
    }

    /**
     * Breadcrumb item object
     */
    private static class BreadCrumbItem extends WebMarkupContainer {

        private BreadCrumbItem(String id, final String crumbTitle, final PageParameters pageParameters, boolean first) {
            super(id);

            add(new AjaxLink("link") {

                @Override
                protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
                    replaceComponentTagBody(markupStream, openTag, crumbTitle);
                }

                @Override
                public void onClick(AjaxRequestTarget target) {
                    setResponsePage(BuildBrowserRootPage.class, pageParameters);
                }
            });
            add(new WebMarkupContainer("sep").setVisible(!first));
        }
    }
}