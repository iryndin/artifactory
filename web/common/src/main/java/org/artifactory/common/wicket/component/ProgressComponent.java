/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 * @author Eli Givoni
 */
public abstract class ProgressComponent extends WebMarkupContainer {
    protected ProgressComponent(String id, final IModel modelObject) {
        super(id);

        add(new AttributeModifier("style", new AbstractReadOnlyModel() {
            @Override
            public Object getObject() {
                Object obj = modelObject.getObject();
                return generateNewAttributeString(getProgress(obj), obj);
            }
        }));
    }

    protected abstract String generateNewAttributeString(Object num, Object password);

    protected abstract double getProgress(Object strPassword);
}
