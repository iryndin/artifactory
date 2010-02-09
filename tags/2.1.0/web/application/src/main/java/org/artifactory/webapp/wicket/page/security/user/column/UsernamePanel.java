/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.wicket.page.security.user.column;

import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.artifactory.common.wicket.component.deletable.listview.DeletableLabelGroup;
import org.artifactory.webapp.wicket.page.security.user.UserModel;

/**
 * @author Yoav Aharoni
 */
public class UsernamePanel extends Panel {
    public UsernamePanel(String id, IModel model) {
        super(id);
        add(new SimpleAttributeModifier("class", "UserColumn"));
        UserModel userModel = (UserModel) model.getObject();
        add(new Label("username", userModel.getUsername()));

        DeletableLabelGroup<String> groups = new DeletableLabelGroup<String>("groups", userModel.getGroups());
        groups.setItemsPerPage(3);
        groups.setLabelClickable(false);
        groups.setLabelDeletable(false);
        add(groups);
    }
}
