package org.artifactory.webapp.wicket.page.security.user.column;

import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.artifactory.webapp.wicket.common.component.deletable.listview.DeletableLabelGroup;
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
