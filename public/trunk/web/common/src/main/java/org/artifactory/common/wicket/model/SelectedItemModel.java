package org.artifactory.common.wicket.model;

import org.apache.wicket.model.IModel;

import java.util.Collection;

/**
 * Boolean model for selecting item in a collection.
 * If model value is true, item is contained in collection.
 * Else, item isn't contained in collection.
 *
 * @author Yoav Aharoni
 */
public class SelectedItemModel<T> implements IModel {
    private Collection<T> items;
    private T item;

    public SelectedItemModel(Collection<T> items, T item) {
        this.items = items;
        this.item = item;
    }

    public Object getObject() {
        return items.contains(item);
    }

    public void setObject(Object object) {
        if ((Boolean) object) {
            if (!items.contains(item)) {
                items.add(item);
            }
        } else {
            items.remove(item);
        }
    }

    public void detach() {
    }
}
