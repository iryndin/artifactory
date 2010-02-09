package org.artifactory.webapp.wicket.common.component.checkbox.styled;

import org.apache.wicket.behavior.HeaderContributor;
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

/**
 *
 */
public class StyledCheckbox extends FormComponentPanel implements Titled {
    private CheckBox checkbox;

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
        };
        checkbox.setOutputMarkupId(true);
        add(checkbox);

        add(new CheckboxButton("button"));
    }

    @SuppressWarnings({"MethodOnlyUsedFromInnerClass"})
    private boolean isChecked() {
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
        Object label = null;

        if (getLabel() != null) {
            label = getLabel().getObject();
        }

        if (label == null) {
            label = getLocalizer().getString(getId(), getParent(), getId());
        }

        return label.toString();
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
                tag.put("class", "styled-checkbox styled-checkbox-disabled");
            } else if (isChecked()) {
                tag.put("class", "styled-checkbox styled-checkbox-checked");
            } else {
                tag.put("class", "styled-checkbox styled-checkbox-unchecked");
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