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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.TextContentPanel;
import org.artifactory.common.wicket.component.label.highlighter.Syntax;
import org.artifactory.common.wicket.component.label.highlighter.SyntaxHighlighter;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.modal.panel.bordered.BorderedModalPanel;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;

/**
 * Base class for actions viewing text resources.
 *
 * @author yoavl
 */
public abstract class ViewAction extends RepoAwareItemAction {
    public static final String ACTION_NAME = "View";

    public ViewAction() {
        this(ACTION_NAME);
    }

    public ViewAction(String actionName) {
        super(actionName);
    }

    protected void showHighlightedSourceModal(RepoAwareItemEvent e, String content, String title) {
        String mimeType = getMimeType(e);
        Syntax syntax = Syntax.fromMimeType(mimeType);
        showHighlightedSourceModal(e, content, title, syntax);
    }

    protected void showHighlightedSourceModal(RepoAwareItemEvent e, String content, String title, Syntax syntax) {
        final String id = e.getTargetComponents().getModalWindow().getContentId();
        showModal(e, title, new SyntaxHighlighter(id, content, syntax));
    }

    protected void showPlainTextModal(RepoAwareItemEvent e, String content, String title, Syntax syntax) {
        final String id = e.getTargetComponents().getModalWindow().getContentId();
        showModal(e, title, new TextContentPanel(id).setContent(content));
    }

    private void showModal(RepoAwareItemEvent e, String title, Component content) {
        ModalWindow modalWindow = e.getTargetComponents().getModalWindow();
        BaseModalPanel modelPanel = new BorderedModalPanel(content);
        modelPanel.setTitle(title);
        content.add(new CssClass("modal-code"));
        modalWindow.setContent(modelPanel);
        AjaxRequestTarget target = e.getTarget();
        modalWindow.show(target);
    }

    protected String getMimeType(RepoAwareItemEvent e) {
        ItemInfo itemInfo = e.getSource().getItemInfo();
        if (itemInfo.isFolder()) {
            return null;
        }
        return ((FileInfo) itemInfo).getMimeType();
    }
}
