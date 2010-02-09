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

package org.artifactory.common.wicket.component;

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
