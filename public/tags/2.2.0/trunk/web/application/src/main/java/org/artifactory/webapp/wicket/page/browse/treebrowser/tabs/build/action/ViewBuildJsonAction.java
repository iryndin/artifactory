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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.action;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.TextContentPanel;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.modal.panel.bordered.BorderedModalPanel;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.action.ViewAction;
import org.artifactory.webapp.actionable.event.ItemEvent;

/**
 * Displays a build JSON in a modal window
 *
 * @author Noam Y. Tenne
 */
public class ViewBuildJsonAction extends ItemAction {

    public static final String ACTION_NAME = "View Build JSON";
    private ModalHandler textContentViewer;
    private String buildJson;

    /**
     * Main constructor
     *
     * @param textContentViewer Modal handler for displaying the build JSON
     * @param buildJson         Build JSON to display
     */
    public ViewBuildJsonAction(ModalHandler textContentViewer, String buildJson) {
        super(ACTION_NAME);
        this.textContentViewer = textContentViewer;
        this.buildJson = buildJson;
    }

    @Override
    public void onAction(ItemEvent e) {
        TextContentPanel contentPanel = new TextContentPanel(textContentViewer.getContentId());
        contentPanel.setContent(buildJson);
        BaseModalPanel modelPanel = new BorderedModalPanel(contentPanel);
        modelPanel.setTitle("Build Info JSON");
        contentPanel.add(new CssClass("modal-code"));
        textContentViewer.setContent(modelPanel);
        AjaxRequestTarget target = e.getTarget();
        textContentViewer.show(target);
    }

    @Override
    public String getCssClass() {
        return ViewAction.class.getSimpleName();
    }
}
