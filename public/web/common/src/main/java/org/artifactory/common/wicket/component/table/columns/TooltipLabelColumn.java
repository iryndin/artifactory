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
