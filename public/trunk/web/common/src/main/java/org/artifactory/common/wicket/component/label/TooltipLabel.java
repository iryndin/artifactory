package org.artifactory.common.wicket.component.label;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.behavior.tooltip.TooltipBehavior;

/**
 * @author Yoav Aharoni
 */
public class TooltipLabel extends Label {
    private int maxLength;

    @WicketProperty
    private transient String tooltip;
    private transient String text;

    public TooltipLabel(String id, String label, int maxLength) {
        this(id, new Model(label), maxLength);
    }

    public TooltipLabel(String id, IModel model, int maxLength) {
        super(id, model);
        this.maxLength = maxLength;
        add(new TooltipBehavior(new PropertyModel(this, "tooltip")));
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        Object modelObject = getModelObject();
        if (modelObject == null) {
            text = null;
            tooltip = null;
            return;
        }

        String modelObjectString = modelObject.toString();
        if (modelObjectString.length() > maxLength) {
            text = getModelObjectAsString(modelObjectString.substring(0, maxLength)) + "...";
            tooltip = getModelObjectAsString(modelObjectString);
        } else {
            text = getModelObjectAsString(modelObjectString);
            tooltip = null;
        }
    }

    @Override
    protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
        replaceComponentTagBody(markupStream, openTag, text);
    }
}
