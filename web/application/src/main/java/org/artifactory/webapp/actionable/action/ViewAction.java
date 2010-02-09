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

package org.artifactory.webapp.actionable.action;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.TextContentPanel;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.modal.panel.bordered.BorderedModalPanel;
import org.artifactory.webapp.actionable.event.ItemEventTargetComponents;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;

/**
 * Base class for actions viewing text resources.
 *
 * @author yoavl
 */
public abstract class ViewAction extends RepoAwareItemAction {
    public static final String ACTION_NAME = "View";

    public ViewAction() {
        super(ACTION_NAME);
    }

    protected void displayModalWindow(RepoAwareItemEvent e, String content, String title) {
        ItemEventTargetComponents eventTargetComponents = e.getTargetComponents();
        ModalWindow textContentViewer = eventTargetComponents.getModalWindow();
        TextContentPanel contentPanel = new TextContentPanel(textContentViewer.getContentId());
        contentPanel.setContent(content);
        BaseModalPanel modelPanel = new BorderedModalPanel(contentPanel);
        modelPanel.setTitle(title);
        contentPanel.add(new CssClass("modal-code"));
        textContentViewer.setContent(modelPanel);
        AjaxRequestTarget target = e.getTarget();
        textContentViewer.show(target);
    }

}
