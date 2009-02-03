package org.artifactory.webapp.wicket.component.deletable.listview;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.DefaultDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.webapp.wicket.component.StringChoiceRenderer;
import org.artifactory.webapp.wicket.component.deletable.label.DeletableLabel;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * @author Yoav Aharoni
 */
public class DeletableLabelGroup<T> extends Panel {
    private IChoiceRenderer renderer;
    private boolean labelClickable = true;
    private boolean labelDeletable = true;
    private DataView dataView;

    public DeletableLabelGroup(String id, IModel collectionModel) {
        this(id, collectionModel, null);
    }

    public DeletableLabelGroup(String id, Collection<T> collection) {
        this(id, new Model((Serializable) collection), null);
    }

    public DeletableLabelGroup(String id, Collection<T> collection, IChoiceRenderer renderer) {
        this(id, new Model((Serializable) collection), renderer);
    }

    public DeletableLabelGroup(String id, IModel collectionModel, IChoiceRenderer renderer) {
        super(id, collectionModel);
        setRenderer(renderer);
        setOutputMarkupId(true);

        dataView = new DataView("item", new LabelsDataProvider()) {
            @SuppressWarnings({"unchecked"})
            @Override
            protected void populateItem(Item item) {
                final T value = (T) item.getModelObject();
                String itemText = getDisplayValue(value);
                item.add(newLabel(value, itemText));
            }
        };
        add(dataView);
        add(new MoreIndicator("more"));
    }

    protected String getDisplayValue(T value) {
        return renderer.getDisplayValue(value).toString();
    }

    public void onDelete(T value, AjaxRequestTarget target) {
        getData().remove(value);
        target.addComponent(this);
    }

    public IChoiceRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(IChoiceRenderer renderer) {
        this.renderer = renderer == null ? StringChoiceRenderer.getInstance() : renderer;
    }

    public boolean isLabelClickable() {
        return labelClickable;
    }

    public void setLabelClickable(boolean labelClickable) {
        this.labelClickable = labelClickable;
    }

    public boolean isLabelDeletable() {
        return labelDeletable;
    }

    public void setLabelDeletable(boolean labelDeletable) {
        this.labelDeletable = labelDeletable;
    }


    public int getItemsPerPage() {
        return dataView.getItemsPerPage();
    }

    public void setItemsPerPage(int maxItems) {
        dataView.setItemsPerPage(maxItems);
    }

    private DeletableLabel newLabel(final T value, final String itemText) {
        DeletableLabel label = new DeletableLabel("label", itemText) {
            @Override
            public void onDeleteClicked(AjaxRequestTarget target) {
                DeletableLabelGroup.this.onDelete(value, target);
            }
        };
        label.setLabelClickable(isLabelClickable());
        label.setLabelDeletable(isLabelDeletable());
        return label;
    }

    @SuppressWarnings({"unchecked"})
    public Collection<T> getData() {
        Collection<T> data = (Collection<T>) getModelObject();
        if (data == null) {
            return Collections.emptyList();
        }
        return data;
    }

    private class LabelsDataProvider extends DefaultDataProvider {
        public Iterator iterator(int first, int count) {
            // no paging anyway...
            return getData().iterator();
        }

        public int size() {
            return getData().size();
        }

        public IModel model(Object object) {
            return new Model((Serializable) object);
        }
    }

    private class MoreIndicator extends WebMarkupContainer {
        public MoreIndicator(String id) {
            super(id);
        }

        @Override
        public boolean isVisible() {
            return super.isVisible() && dataView.getPageCount() > 1;
        }
    }
}
