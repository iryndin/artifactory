package org.artifactory.common.wicket.panel;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * @author Yoav Aharoni
 */
public class BasePanel<T extends Serializable> extends Panel {
    public BasePanel(String id) {
        super(id);
    }

    public BasePanel(String id, T object) {
        this(id, new CompoundPropertyModel(object));
    }

    public BasePanel(String id, IModel model) {
        super(id, model);
    }

    {
        setOutputMarkupId(true);
    }

    @SuppressWarnings({"unchecked"})
    public T getPanelModelObject() {
        return (T) getModelObject();
    }

    @SuppressWarnings({"TypeMayBeWeakened"})
    public void setPanelModelObject(T value) {
        setModelObject(value);
    }
}
