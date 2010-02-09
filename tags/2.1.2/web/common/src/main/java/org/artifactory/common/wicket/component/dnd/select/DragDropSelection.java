/*
 * This file is part of Artifactory.
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

package org.artifactory.common.wicket.component.dnd.select;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.IBehavior;
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
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.StringChoiceRenderer;
import org.artifactory.common.wicket.component.dnd.list.DragDropList;
import org.artifactory.common.wicket.component.links.SimpleTitledLink;
import org.artifactory.common.wicket.component.template.HtmlTemplate;
import org.artifactory.common.wicket.contributor.ResourcePackage;
import org.artifactory.common.wicket.resources.basewidget.BaseWidget;

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
    private List<T> unselectedItems;

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

    public DragDropSelection(final String id, IModel model, final List<T> choices, final IChoiceRenderer renderer) {
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
        ResourcePackage resourcePackage = ResourcePackage.forJavaScript(DragDropSelection.class);
        resourcePackage.dependsOn(new BaseWidget());
        add(resourcePackage);

        add(new CssClass(new PropertyModel(this, "cssClass")));
        setOutputMarkupId(true);

        BaseDragDropList sourceList = new SourceDragDropList("sourceList");
        add(sourceList);

        BaseDragDropList targetList = new TargetDragDropList("targetList");
        add(targetList);

        HiddenField selectionField = new SelectionField("selection");
        add(selectionField);

        SimpleTitledLink addLink = new SimpleTitledLink("addLink", ">>");
        add(addLink);

        SimpleTitledLink removeLink = new SimpleTitledLink("removeLink", "<<");
        add(removeLink);

        add(new Label("sourceTitle", new TitleModel("selection.source")));
        add(new Label("targetTitle", new TitleModel("selection.target")));

        // add init script
        HtmlTemplate template = new HtmlTemplate("initScript") {
            @Override
            public boolean isVisible() {
                return super.isVisible() && isScriptRendered();
            }
        };
        template.setParameter("widgetClassName", new PropertyModel(this, "widgetClassName"));
        template.setParameter("panelId", new PropertyModel(this, "markupId"));
        template.setParameter("sourceListId", new PropertyModel(sourceList, "markupId"));
        template.setParameter("targetListId", new PropertyModel(targetList, "markupId"));
        template.setParameter("addLinkId", new PropertyModel(addLink, "markupId"));
        template.setParameter("removeLinkId", new PropertyModel(removeLink, "markupId"));
        template.setParameter("textFieldId", new PropertyModel(selectionField, "markupId"));
        add(template);
    }

    public String getCssClass() {
        if (isEnabled()) {
            return "dnd-selection";
        } else {
            return "dnd-selection disabled";
        }
    }

    protected boolean isScriptRendered() {
        return isEnabled();
    }

    public String getWidgetClassName() {
        return "artifactory.DragDropSelection";
    }

    protected String getDndValue(ListItem item) {
        return getMarkupId();
    }

    protected String getAcceptedSourceTypes() {
        return getMarkupId();
    }

    protected String getAcceptedTargetTypes() {
        return getMarkupId();
    }

    protected void onOrderChanged(AjaxRequestTarget target) {
    }

    protected IBehavior newOnOrderChangeEventBehavior(String event) {
        return new OnOrderChangedEventBehavior(event);
    }

    @SuppressWarnings({"unchecked"})
    protected void populateItem(ListItem item) {
        T itemObject = (T) item.getModelObject();
        List<T> choices = (List<T>) choicesModel.getObject();
        int index = choices.indexOf(itemObject);
        item.add(new SimpleAttributeModifier("idx", String.valueOf(index)));
    }

    protected Collection<T> createNewSelectionCollection(int length) {
        if (length == 0) {
            return Collections.emptyList();
        }
        return new ArrayList<T>(length);
    }

    @Override
    public void updateModel() {
        // do nothing, model is updated by TargetSelectionModel
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
        unselectedItems = new ArrayList<T>((Collection<T>) choicesModel.getObject());
        Collection<T> selected = (Collection<T>) getModelObject();
        if (isNotEmpty(selected)) {
            unselectedItems.removeAll(selected);
        }
    }

    protected String getSortValue(ListItem item) {
        return null;
    }

    private class SourceListModel extends AbstractReadOnlyModel {
        @Override
        public Object getObject() {
            return unselectedItems;
        }
    }

    private class TargetListModel extends AbstractReadOnlyModel {
        @Override
        public Object getObject() {
            Collection<?> selected = (Collection<?>) getModelObject();
            if (selected instanceof List) {
                return selected;
            }
            if (isEmpty(selected)) {
                return Collections.emptyList();
            }
            return new ArrayList<Object>(selected);
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

    private abstract class BaseDragDropList extends DragDropList {
        private BaseDragDropList(String id, IModel listModel) {
            super(id, listModel, getChoiceRenderer());
            setOutputMarkupId(true);
        }

        @Override
        protected String getSortValue(ListItem item) {
            return DragDropSelection.this.getSortValue(item);
        }

        @Override
        public String getDndValue(ListItem item) {
            return DragDropSelection.this.getDndValue(item);
        }

        @Override
        protected void populateItem(ListItem item) {
            super.populateItem(item);
            DragDropSelection.this.populateItem(item);
        }
    }

    private class SourceDragDropList extends BaseDragDropList {
        private SourceDragDropList(String id) {
            super(id, new SourceListModel());
        }

        @Override
        public String getAcceptedDndTypes() {
            return getAcceptedSourceTypes();
        }
    }

    private class TargetDragDropList extends BaseDragDropList {
        private TargetDragDropList(String id) {
            super(id, new TargetListModel());
        }

        @Override
        public String getAcceptedDndTypes() {
            return getAcceptedTargetTypes();
        }
    }

    public class OnOrderChangedEventBehavior extends AjaxFormComponentUpdatingBehavior {
        private OnOrderChangedEventBehavior(String event) {
            super(event);
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
            onOrderChanged(target);
        }
    }

    private class TargetSelectionModel extends Model {
        private TargetSelectionModel() {
            // make sure model will notify change for empty selection
            super("-");
        }

        @Override
        @SuppressWarnings({"unchecked"})
        public void setObject(Object object) {
            super.setObject(object);
            if (object == null) {
                setModelObject(createNewSelectionCollection(0));
                return;
            }

            String selectionString = object.toString();
            String[] selectedIndices = selectionString.split(",");

            // get newSelection list
            Collection<T> newSelection = createNewSelectionCollection(selectedIndices.length);

            // fill newSelection
            List<T> choices = (List<T>) choicesModel.getObject();
            for (String index : selectedIndices) {
                Integer intIndex = Integer.valueOf(index);
                newSelection.add(choices.get(intIndex));
            }
            setModelObject(newSelection);
        }
    }

    private class SelectionField extends HiddenField {
        private SelectionField(String id) {
            super(id, new TargetSelectionModel());
            setOutputMarkupId(true);
            add(newOnOrderChangeEventBehavior("onOrderChanged"));
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() && DragDropSelection.this.isEnabled();
        }
    }
}
