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

package org.artifactory.webapp.actionable.event;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;

import java.io.Serializable;

/**
 * @author yoavl
 */
public class ItemEventTargetComponents implements Serializable {

    protected final Component refreshableComponent;
    protected final WebMarkupContainer nodePanelContainer;
    protected final ModalWindow modalWindow;

    public ItemEventTargetComponents() {
        refreshableComponent = null;
        nodePanelContainer = null;
        modalWindow = null;
    }

    public ItemEventTargetComponents(
            Component refreshableComponent,
            WebMarkupContainer nodePanelContainer,
            ModalWindow modalWindow) {
        this.refreshableComponent = refreshableComponent;
        this.nodePanelContainer = nodePanelContainer;
        this.modalWindow = modalWindow;
    }

    /**
     * @return The component to refresh when an action is finished.
     */
    public Component getRefreshableComponent() {
        return refreshableComponent;
    }

    /**
     * @return The panel containing a selected item when the event is fired from the tree. Null
     *         if the event is not fired from the tree (e.g., from the search page)
     */
    public WebMarkupContainer getNodePanelContainer() {
        return nodePanelContainer;
    }

    /**
     * @return A ModalWindow the action can use to display content.
     */
    public ModalWindow getModalWindow() {
        return modalWindow;
    }
}