package org.artifactory.common.wicket.component.table.columns;

import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.artifactory.common.wicket.component.label.TooltipLabel;

/**
 * @author Yoav Aharoni
 */
public class TooltipLabelColumn extends PropertyColumn {
    private int maxLength;

    public TooltipLabelColumn(IModel displayModel, String sortProperty, String propertyExpression, int maxLength) {
        super(displayModel, sortProperty, propertyExpression);
        this.maxLength = maxLength;
    }

    public TooltipLabelColumn(IModel displayModel, String propertyExpression, int maxLength) {
        super(displayModel, propertyExpression);
        this.maxLength = maxLength;
    }

    @Override
    public void populateItem(Item item, String componentId, IModel model) {
        item.add(new TooltipLabel(componentId, createLabelModel(model), maxLength));
    }
}
