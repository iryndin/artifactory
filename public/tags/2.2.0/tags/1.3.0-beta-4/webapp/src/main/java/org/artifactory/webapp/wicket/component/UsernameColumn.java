package org.artifactory.webapp.wicket.component;

import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author Yoav Aharoni
 */
public class UsernameColumn extends PropertyColumn {
    public UsernameColumn(String propertyExpression) {
        this(new Model("Username"), propertyExpression);
    }

    public UsernameColumn(IModel displayModel, String propertyExpression) {
        super(displayModel, propertyExpression, propertyExpression);
    }

    @Override
    public void populateItem(Item item, String componentId, IModel model) {
        super.populateItem(item, componentId, model);
    }
}
