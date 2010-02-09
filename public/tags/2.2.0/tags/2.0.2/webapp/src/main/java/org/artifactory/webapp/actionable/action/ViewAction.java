/**
 *  Artifactory by jfrog [http://artifactory.jfrog.org]
 *  Copyright (C) 2000-2008 jfrog Ltd.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/> or write to
 *  the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA 02110-1301 USA.
 *
 *  You can also contact jfrog Ltd. at info@jfrog.org.
 *
 *  The interactive user interfaces in modified source and object code versions
 *  of this program must display Appropriate Legal Notices, as required under
 *  Section 5 of the GNU Affero General Public License version 3.
 *
 *  In accordance with Section 7(b) of the GNU Affero General Public License
 *  version 3, these Appropriate Legal Notices must retain the display of the
 *  "Powered by Artifactory" logo. If the display of the logo is not reasonably
 *  feasible for technical reasons, the Appropriate Legal Notices must display
 *  the words "Powered by Artifactory".
 */

package org.artifactory.webapp.actionable.action;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.event.ItemEventTargetComponents;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;
import org.artifactory.webapp.wicket.common.component.TextContentPanel;
import org.artifactory.webapp.wicket.common.component.modal.panel.bordered.BorderedModelPanel;

/**
 * @author yoavl
 */
public class ViewAction extends RepoAwareItemAction {
    public static final String ACTION_NAME = "View";

    public ViewAction() {
        super(ACTION_NAME, null);
    }

    @Override
    public void onAction(RepoAwareItemEvent e) {
        RepoAwareActionableItem source = e.getSource();
        ItemInfo info = source.getItemInfo();
        String content = getRepoService().getPomContent(info);
        ItemEventTargetComponents eventTargetComponents = e.getTargetComponents();
        ModalWindow textContentViewer = eventTargetComponents.getModalWindow();
        TextContentPanel contentPanel =
                new TextContentPanel(textContentViewer.getContentId());
        contentPanel.setContent(content);
        BorderedModelPanel modelPanel = new BorderedModelPanel(contentPanel);
        modelPanel.setTitle(info.getName());
        textContentViewer.setContent(modelPanel);
        AjaxRequestTarget target = e.getTarget();
        textContentViewer.show(target);
    }
}