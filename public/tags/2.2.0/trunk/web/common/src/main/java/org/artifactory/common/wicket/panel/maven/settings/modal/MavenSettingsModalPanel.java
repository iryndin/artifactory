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

package org.artifactory.common.wicket.panel.maven.settings.modal;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.target.resource.ResourceStreamRequestTarget;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;

/**
 * A custom modal display panel for the generated maven settings. Created for the need of customizing the modal window
 * content panel, so an export link could be added
 *
 * @author Noam Y. Tenne
 */
public class MavenSettingsModalPanel extends BaseModalPanel {

    /**
     * Main constructor
     *
     * @param settings Maven settings content
     */
    public MavenSettingsModalPanel(final String settings) {
        setWidth(700);

        MarkupContainer border = new TitledBorder("border");
        Component contentPanel = new Label("settings", settings).setEscapeModelStrings(false);
        contentPanel.add(new CssClass("modal-code"));

        border.add(contentPanel);
        add(border);

        Component exportLink = new Link("export") {
            @Override
            public void onClick() {
                IResourceStream resourceStream =
                        new StringResourceStream(settings, "application/xml");
                getRequestCycle().setRequestTarget(new ResourceStreamRequestTarget(resourceStream) {
                    @Override
                    public String getFileName() {
                        return "settings.xml";
                    }
                });
            }
        };
        add(exportLink);
    }

    @Override
    public void onShow(AjaxRequestTarget target) {
        super.onShow(target);
        target.appendJavascript("ModalHandler.bindModalHeight(dojo.byId('mavenSettings'));");
    }

    @Override
    public String getCookieName() {
        return null;
    }
}
