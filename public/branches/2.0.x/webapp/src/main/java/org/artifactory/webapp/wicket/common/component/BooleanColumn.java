package org.artifactory.webapp.wicket.common.component;

import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.artifactory.webapp.wicket.common.behavior.CssClass;

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
        IModel booleanModel = createLabelModel(model);
        Boolean value = (Boolean) booleanModel.getObject();
        Label label = new Label(componentId, "<span>" + value + "</span>");
        label.setEscapeModelStrings(false);
        item.add(label);

        item.add(new CssClass("BooleanColumn"));
        if (value != null) {
            item.add(new CssClass(value.toString().toLowerCase()));
        }
    }
}
