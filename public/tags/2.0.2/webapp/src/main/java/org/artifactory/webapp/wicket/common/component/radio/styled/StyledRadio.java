package org.artifactory.webapp.wicket.common.component.radio.styled;

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.LabeledWebMarkupContainer;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Objects;
import org.artifactory.webapp.wicket.common.Titled;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.behavior.DelegateEventBehavior;

/**
 *
 */
public class StyledRadio extends LabeledWebMarkupContainer implements Titled {
    private Radio radio;
    private Component button;

    public StyledRadio(String id) {
        super(id);
        init();
    }

    public StyledRadio(String id, IModel model) {
        super(id, model);
        init();
    }

    protected void init() {
        add(HeaderContributor.forJavaScript(StyledRadio.class, "StyledRadio.js"));
        add(new CssClass("styled-checkbox"));

        radio = new Radio("radio", new DelegetedModel());
        radio.setOutputMarkupId(true);
        add(radio);

        button = new RadioButton("button");
        add(button);
    }

    @Override
    public Component add(IBehavior behavior) {
        if (AjaxEventBehavior.class.isAssignableFrom(behavior.getClass())) {
            AjaxEventBehavior ajaxEventBehavior = (AjaxEventBehavior) behavior;
            button.add(new DelegateEventBehavior(ajaxEventBehavior.getEvent(), radio));
            radio.add(ajaxEventBehavior);
            return this;
        }

        return super.add(behavior);
    }

    @SuppressWarnings({"MethodOnlyUsedFromInnerClass"})
    private boolean isChecked() {
        RadioGroup group = (RadioGroup) findParent(RadioGroup.class);
        if (group == null) {
            throw new WicketRuntimeException(
                    "Radio component ["
                            + getPath()
                            + "] cannot find its parent RadioGroup. All Radio components must be a child of or below in the hierarchy of a RadioGroup component.");
        }

        if (group.hasRawInput()) {
            String rawInput = group.getRawInput();
            if (rawInput != null && rawInput.equals(radio.getValue())) {
                return true;
            }
        } else if (Objects.equal(group.getModelObject(), getModelObject())) {
            return true;
        }

        return false;
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        checkComponentTag(tag, "input");
        checkComponentTagAttribute(tag, "type", "radio");

        // rename input tag to span tag
        tag.setName("span");
        tag.remove("type");
        tag.remove("value");
        tag.remove("name");
    }

    @Override
    protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
        super.onComponentTagBody(markupStream, openTag);

        // close span  tag
        getResponse().write(openTag.syntheticCloseTagString());
        openTag.setType(XmlTag.CLOSE);
    }

    public String getTitle() {
        Object label = null;

        if (getLabel() != null) {
            label = getLabel().getObject();
        }

        if (label == null) {
            label = getLocalizer().getString(getId(), getParent(), getId());
        }

        return label.toString();
    }

    private class RadioButton extends WebMarkupContainer {
        private RadioButton(String id) {
            super(id, new Model());
        }

        @Override
        protected void onComponentTag(ComponentTag tag) {
            super.onComponentTag(tag);
            tag.put("for", radio.getMarkupId());

            if (isChecked()) {
                tag.put("class", "styled-checkbox styled-checkbox-checked");
            } else {
                tag.put("class", "styled-checkbox styled-checkbox-unchecked");
            }
        }

        @SuppressWarnings({"RefusedBequest"})
        @Override
        protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
            replaceComponentTagBody(markupStream, openTag, getHtml());
        }

        private String getHtml() {
            return "<div class='button-center'><div class='button-left'><div class='button-right'>"
                    + getTitle()
                    + "</div></div></div>";
        }
    }

    private class DelegetedModel implements IModel {
        public Object getObject() {
            return getModelObject();
        }

        public void setObject(Object object) {
            setModelObject(object);
        }

        public void detach() {
        }
    }

}
