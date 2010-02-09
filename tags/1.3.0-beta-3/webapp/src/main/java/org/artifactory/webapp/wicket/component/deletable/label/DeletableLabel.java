package org.artifactory.webapp.wicket.component.deletable.label;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.webapp.wicket.behavior.CssClass;

/**
 * @author Yoav Aharoni
 */
public abstract class DeletableLabel extends Panel {
    private boolean labelClickable = false;
    private boolean labelDeletable = false;

    public DeletableLabel(String id, String text) {
        this(id, new Model(text));
    }

    public DeletableLabel(String id, IModel model) {
        super(id, model);

        add(new CssClass(new AbstractReadOnlyModel() {
            public Object getObject() {
                return isLabelDeletable() ? "deletable" : "deletable undeletable";
            }
        }));

        Label label = new Label("label", new DeletegeModel());
        label.add(new AjaxEventBehavior("onclick") {
            protected void onEvent(AjaxRequestTarget target) {
                onLabelClicked(target);
            }

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                tag.put("onmouseover", "DeletableLabel.setClass(this, 'overlabel')");
                tag.put("onmouseout", "DeletableLabel.setClass(this, '')");
            }

            @Override
            public boolean isEnabled(Component component) {
                return super.isEnabled(component) && isLabelClickable();
            }
        });
        add(label);

        add(new AjaxLink("link") {
            public void onClick(AjaxRequestTarget target) {
                onDeleteClicked(target);
            }

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                tag.put("onmouseover", "DeletableLabel.setClass(this, 'overlink')");
                tag.put("onmouseout", "DeletableLabel.setClass(this, '')");
            }

        });
    }

    public void onDeleteClicked(AjaxRequestTarget target) {
    }

    public void onLabelClicked(AjaxRequestTarget target) {
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

    private class DeletegeModel extends AbstractReadOnlyModel {
        public Object getObject() {
            return DeletableLabel.this.getModelObject();
        }
    }
}
