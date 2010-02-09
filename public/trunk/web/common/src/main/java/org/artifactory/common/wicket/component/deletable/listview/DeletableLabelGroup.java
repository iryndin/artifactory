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

package org.artifactory.common.wicket.component.deletable.listview;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.DefaultDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.api.security.UserInfo;
import org.artifactory.common.wicket.component.StringChoiceRenderer;
import org.artifactory.common.wicket.component.deletable.label.DeletableLabel;

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

    public boolean isLabelClickable(T value) {
        return labelClickable;
    }

    public void setLabelClickable(boolean labelClickable) {
        this.labelClickable = labelClickable;
    }

    public boolean isLabelDeletable(T value) {
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
                onDelete(value, target);
            }
        };
        if (value instanceof UserInfo.UserGroupInfo) {
            UserInfo.UserGroupInfo userGroupInfo = (UserInfo.UserGroupInfo) value;
            label.setLabelClickable(!userGroupInfo.isExternal());
            label.setLabelDeletable(!userGroupInfo.isExternal());
        } else {
            label.setLabelClickable(isLabelClickable(value));
            label.setLabelDeletable(isLabelDeletable(value));
        }
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
        private MoreIndicator(String id) {
            super(id);
        }

        @Override
        public boolean isVisible() {
            return super.isVisible() && dataView.getPageCount() > 1;
        }
    }
}
