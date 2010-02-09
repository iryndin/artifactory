package org.artifactory.webapp.wicket.common.component.dnd.select;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.StringChoiceRenderer;
import org.artifactory.webapp.wicket.common.component.dnd.list.DragDropList;
import org.artifactory.webapp.wicket.common.component.template.HtmlTemplate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class DragDropSelection<T> extends FormComponentPanel {
    private IModel choicesModel;
    private IChoiceRenderer renderer;
    private List<T> sourceList;

    @SuppressWarnings({"unchecked"})
    public DragDropSelection(final String id) {
        this(id, Collections.<T>emptyList());
    }

    public DragDropSelection(final String id, final List<T> choices) {
        this(id, new Model((Serializable) choices));
    }

    public DragDropSelection(final String id, final List<T> choices, final IChoiceRenderer renderer) {
        this(id, new Model((Serializable) choices), renderer);
    }


    public DragDropSelection(final String id, IModel model, final List<T> choices) {
        this(id, model, new Model((Serializable) choices));
    }


    public DragDropSelection(final String id, IModel model, final List<T> choices,
                             final IChoiceRenderer renderer) {
        this(id, model, new Model((Serializable) choices), renderer);
    }


    public DragDropSelection(String id, IModel choicesModel) {
        this(id, choicesModel, StringChoiceRenderer.getInstance());
    }


    public DragDropSelection(String id, IModel model, IModel choicesModel) {
        this(id, model, choicesModel, StringChoiceRenderer.getInstance());
    }


    public DragDropSelection(String id, IModel choicesModel, IChoiceRenderer renderer) {
        super(id);
        this.choicesModel = choicesModel;
        this.renderer = renderer;
        init();
    }

    public DragDropSelection(String id, IModel model, IModel choicesModel, IChoiceRenderer renderer) {
        super(id, model);
        this.choicesModel = choicesModel;
        this.renderer = renderer;
        init();
    }

    protected void init() {
        add(HeaderContributor.forJavaScript(DragDropSelection.class, "DragDropSelection.js"));
        add(new CssClass("dnd-selection"));
        setOutputMarkupId(true);

        add(new MyDragDropList<T>("sourceList", new SourceListModel()));

        MyDragDropList<T> targetList = new MyDragDropList<T>("targetList", new TargetListModel());
        add(targetList);

        HiddenField textField = new HiddenField("selection", new TargetSelectionModel());
        textField.setOutputMarkupId(true);
        textField.add(newOnOrderChangeEventBehavior());
        add(textField);

        add(new Label("sourceTitle", new TitleModel("selection.source")));
        add(new Label("targetTitle", new TitleModel("selection.target")));

        // add init script
        HtmlTemplate template = new HtmlTemplate("initScript");
        template.setParameter("panelId", new MarkupIdModel());
        template.setParameter("targetListId", new PropertyModel(targetList, "markupId"));
        template.setParameter("textFieldId", new PropertyModel(textField, "markupId"));
        add(template);
    }

    @Override
    public void updateModel() {
        // do nothing, model is updated by TargetSelectionModel
    }

    protected void onOrderChanged(AjaxRequestTarget target) {
    }

    protected OnOrderChangedEventBehavior newOnOrderChangeEventBehavior() {
        return new OnOrderChangedEventBehavior();
    }

    public IModel getChoices() {
        return choicesModel;
    }

    public void setChoices(IModel choices) {
        choicesModel = choices;
    }

    public void setChoices(List<T> choices) {
        choicesModel = new Model((Serializable) choices);
    }

    public IChoiceRenderer getChoiceRenderer() {
        return renderer;
    }

    public void setChoiceRenderer(IChoiceRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    protected void onBeforeRender() {
        updateSourceList();
        super.onBeforeRender();
    }

    @SuppressWarnings({"unchecked"})
    private void updateSourceList() {
        sourceList = new ArrayList<T>((Collection<T>) choicesModel.getObject());
        List<T> selected = (List<T>) getModelObject();
        sourceList.removeAll(selected);
    }

    @SuppressWarnings({"unchecked"})
    protected void populateItem(ListItem item) {
        T itemObject = (T) item.getModelObject();
        List<T> choices = (List<T>) choicesModel.getObject();
        int index = choices.indexOf(itemObject);
        item.add(new SimpleAttributeModifier("idx", String.valueOf(index)));
    }

    private class SourceListModel extends AbstractReadOnlyModel {
        @Override
        public Object getObject() {
            return sourceList;
        }
    }

    private class TargetListModel extends AbstractReadOnlyModel {
        @Override
        public Object getObject() {
            return getModelObject();
        }
    }

    private class MarkupIdModel extends AbstractReadOnlyModel {
        @Override
        public Object getObject() {
            return getMarkupId();
        }
    }

    private class TitleModel extends AbstractReadOnlyModel {
        private String key;

        private TitleModel(String key) {
            this.key = key;
        }

        @Override
        public Object getObject() {
            String globalTitle = getString(key, null, key);
            return getString(getMarkupId() + "." + key, null, globalTitle);
        }
    }

    private class MyDragDropList<T> extends DragDropList<T> {
        private MyDragDropList(String id, IModel listModel) {
            super(id, listModel, new MarkupIdModel(), getChoiceRenderer());
            setOutputMarkupId(true);
        }

        @Override
        protected void populateItem(ListItem item) {
            super.populateItem(item);
            DragDropSelection.this.populateItem(item);
        }
    }

    public class OnOrderChangedEventBehavior extends AjaxFormComponentUpdatingBehavior {
        private OnOrderChangedEventBehavior() {
            super("onOrderChanged");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
            onOrderChanged(target);
        }
    }

    private class TargetSelectionModel extends Model {
        @Override
        @SuppressWarnings({"unchecked"})
        public void setObject(Object object) {
            super.setObject(object);
            if (object == null) {
                setModelObject(Collections.emptyList());
                return;
            }

            String selectionString = object.toString();
            String[] selectedIndices = selectionString.split(",");
            List<T> newListValue = new ArrayList<T>(selectedIndices.length);
            List<T> choices = (List<T>) choicesModel.getObject();
            for (String index : selectedIndices) {
                Integer intIndex = Integer.valueOf(index);
                newListValue.add(choices.get(intIndex));
            }
            setModelObject(newListValue);
        }

    }
}
