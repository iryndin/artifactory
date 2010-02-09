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

package org.artifactory.common.wicket.component.dnd.list;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.common.wicket.behavior.CssClass;

/**
 * @author Yoav Aharoni
 */
public abstract class DragDropList extends Panel {
    private IChoiceRenderer renderer;

    public DragDropList(String id, IModel listModel, IChoiceRenderer renderer) {
        super(id, listModel);
        this.renderer = renderer;

        add(new SimpleAttributeModifier("dojoType", "artifactory.dnd.Source"));
        add(new AttributeAppender("accept", true, new PropertyModel(this, "acceptedDndTypes"), ","));
        add(new DndListView("list", listModel));
    }

    public abstract String getAcceptedDndTypes();

    public abstract String getDndValue(ListItem item);

    protected void populateItem(ListItem item) {
        item.add(new SimpleAttributeModifier("dndType", getDndValue(item)));
        item.add(new CssClass("dojoDndItem"));

        String displayValue = renderer.getDisplayValue(item.getModelObject()).toString();
        item.add(new Label("value", displayValue));

        String sortValue = getSortValue(item);
        Label sortLabel = new Label("sortValue", sortValue);
        sortLabel.setVisible(sortValue != null);
        item.add(sortLabel);
    }

    protected String getSortValue(ListItem item) {
        return null;
    }

    private class DndListView extends ListView {
        private DndListView(String id, IModel model) {
            super(id, model);
        }

        @Override
        protected void populateItem(ListItem item) {
            DragDropList.this.populateItem(item);
        }
    }


}
