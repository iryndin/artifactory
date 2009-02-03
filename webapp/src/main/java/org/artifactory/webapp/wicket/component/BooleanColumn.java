package org.artifactory.webapp.wicket.component;

import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.artifactory.webapp.wicket.behavior.CssClass;

/**
 * @author Yoav Aharoni
 */
public class BooleanColumn extends PropertyColumn {
    public BooleanColumn(IModel displayModel, String sortProperty, String propertyExpression) {
        super(displayModel, sortProperty, propertyExpression);
    }

    public BooleanColumn(IModel displayModel, String propertyExpression) {
        super(displayModel, propertyExpression);
    }

    @Override
    public void populateItem(Item item, String componentId, IModel model) {
        super.populateItem(item, componentId, model);
        IModel booleanModel = createLabelModel(model);
        Boolean value = (Boolean) booleanModel.getObject();
        item.add(new CssClass("BooleanColumn"));
        if (value != null) {
            item.add(new CssClass(value.toString()));
        }
    }
}
