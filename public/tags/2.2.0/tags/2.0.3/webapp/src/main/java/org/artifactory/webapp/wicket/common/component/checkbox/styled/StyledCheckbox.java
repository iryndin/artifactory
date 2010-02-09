package org.artifactory.webapp.wicket.common.component.checkbox.styled;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.webapp.wicket.common.Titled;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.behavior.DelegateEventBehavior;

/**
 *
 */
public class StyledCheckbox extends FormComponentPanel implements Titled {
    private CheckBox checkbox;
    private Component button;
    private String title = null;

    public StyledCheckbox(String id) {
        super(id);
        init();
    }

    public StyledCheckbox(String id, IModel model) {
        super(id, model);
        init();
    }

    protected void init() {
        add(HeaderContributor.forJavaScript(StyledCheckbox.class, "StyledCheckbox.js"));
        add(new CssClass("styled-checkbox"));

        checkbox = new CheckBox("checkbox", new DelegetedModel()) {
            @Override
            public boolean isEnabled() {
                return super.isEnabled() && StyledCheckbox.this.isEnabled();
            }

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                if (isEnabled()) {
                    tag.put("onclick", "StyledCheckbox.update(this);");
                }
            }
        };
        checkbox.setOutputMarkupId(true);
        add(checkbox);

        button = new CheckboxButton("button");
        add(button);
    }

    @Override
    public Component add(IBehavior behavior) {
        if (AjaxEventBehavior.class.isAssignableFrom(behavior.getClass())) {
            AjaxEventBehavior ajaxEventBehavior = (AjaxEventBehavior) behavior;
            button.add(new DelegateEventBehavior(ajaxEventBehavior.getEvent(), checkbox));
            checkbox.add(ajaxEventBehavior);
            return this;
        }

        return super.add(behavior);
    }

    public boolean isChecked() {
        return Boolean.TRUE.equals(getModelObject());
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        checkComponentTag(tag, "input");
        checkComponentTagAttribute(tag, "type", "checkbox");

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
        if (title == null) {
            Object label = null;
            if (getLabel() != null) {
                label = getLabel().getObject();
            }

            if (label == null) {
                label = getLocalizer().getString(getId(), getParent(), getId());
            }
            title = label.toString();
        }
        return title;
    }

    public StyledCheckbox setTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    public void updateModel() {
        checkbox.updateModel();
    }

    private class CheckboxButton extends WebMarkupContainer {
        private CheckboxButton(String id) {
            super(id, new Model());
        }

        @Override
        protected void onComponentTag(ComponentTag tag) {
            super.onComponentTag(tag);
            tag.put("for", checkbox.getMarkupId());

            if (!isEnabled()) {
                if (isChecked()) {
                    tag.put("class", "styled-checkbox styled-checkbox-disabled-checked");
                } else {
                    tag.put("class", "styled-checkbox styled-checkbox-disabled-unchecked");
                }
            } else {
                if (isChecked()) {
                    tag.put("class", "styled-checkbox styled-checkbox-checked");
                } else {
                    tag.put("class", "styled-checkbox styled-checkbox-unchecked");
                }
            }

            if (StyledCheckbox.this.isEnabled()) {
                tag.put("onmouseover", "StyledCheckbox.onmouseover(this);");
                tag.put("onmouseout", "StyledCheckbox.onmouseout(this);");
                tag.put("onclick", "StyledCheckbox.onclick(this);");
            }
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() && StyledCheckbox.this.isEnabled();
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