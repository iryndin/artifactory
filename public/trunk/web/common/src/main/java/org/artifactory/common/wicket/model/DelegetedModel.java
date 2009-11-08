package org.artifactory.common.wicket.model;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

/**
 * @author Yoav Aharoni
 */
public class DelegetedModel implements IModel {
    private Component component;

    public DelegetedModel(Component component) {
        this.component = component;
    }

    public Object getObject() {
        return component.getModelObject();
    }

    public void setObject(Object object) {
        component.setModelObject(object);
    }

    public void detach() {
    }
}
