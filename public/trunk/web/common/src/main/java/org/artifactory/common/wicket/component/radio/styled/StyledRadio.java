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

package org.artifactory.common.wicket.component.radio.styled;

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxEventBehavior;
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
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.behavior.DelegateEventBehavior;
import org.artifactory.common.wicket.contributor.ResourcePackage;
import org.artifactory.common.wicket.model.DelegetedModel;
import org.artifactory.common.wicket.model.Titled;

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
        add(ResourcePackage.forJavaScript(StyledRadio.class));
        add(new CssClass("styled-checkbox"));

        radio = new Radio("radio", new DelegetedModel(this));
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
                            +
                            "] cannot find its parent RadioGroup. All Radio components must be a child of or below in the hierarchy of a RadioGroup component.");
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
}
