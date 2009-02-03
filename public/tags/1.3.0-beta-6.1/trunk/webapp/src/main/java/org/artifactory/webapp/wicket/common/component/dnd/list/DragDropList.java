package org.artifactory.webapp.wicket.common.component.dnd.list;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.webapp.wicket.common.behavior.CssClass;

import java.io.Serializable;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class DragDropList<T> extends Panel {
    private IChoiceRenderer renderer;
    private IModel dndTypeModel;

    public DragDropList(String id, List<T> list, IModel dndTypeModel, IChoiceRenderer renderer) {
        this(id, new Model((Serializable) list), dndTypeModel, renderer);
    }

    public DragDropList(String id, IModel listModel, IModel dndTypeModel, IChoiceRenderer renderer) {
        super(id, listModel);
        this.renderer = renderer;
        this.dndTypeModel = dndTypeModel;

        add(new SimpleAttributeModifier("dojoType", "dojo.dnd.Source"));
        add(new AttributeAppender("accept", true, dndTypeModel, ","));
        add(new DndListView("list", listModel));
    }

    protected void populateItem(ListItem item) {
        String displayValue = renderer.getDisplayValue(item.getModelObject()).toString();
        item.add(new Label("value", displayValue));
        item.add(new AttributeModifier("dndType", true, dndTypeModel));
        item.add(new CssClass("dojoDndItem"));
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
