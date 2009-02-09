package org.artifactory.webapp.wicket.page.security.user.column;

import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

/**
 * @author Yoav Aharoni
 */
public class UserColumn extends PropertyColumn {
    public UserColumn(IModel displayModel) {
        super(displayModel, "username", "username");
    }

    @Override
    public void populateItem(Item item, String componentId, IModel model) {
        item.add(new UsernamePanel(componentId, model));
    }
}
