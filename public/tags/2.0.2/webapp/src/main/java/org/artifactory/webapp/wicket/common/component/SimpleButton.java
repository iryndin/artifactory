/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.wicket.common.component;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupException;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.Model;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class SimpleButton extends AjaxFallbackButton {
    private static final long serialVersionUID = 1L;

    private boolean wasOpenCloseTag;

    protected SimpleButton(String id, Form form) {
        this(id, form, null);
    }

    protected SimpleButton(String id, Form form, String caption) {
        super(id, caption == null ? null : new Model(caption), form);
        setOutputMarkupId(true);
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        if (tag.isOpenClose()) {
            wasOpenCloseTag = true;
            tag.setType(XmlTag.OPEN);
        }
        if ("a".equals(tag.getName())) {
            tag.put("href", "#");
        }
        tag.put("class", getCssClass(tag));
        super.onComponentTag(tag);
    }

    protected String getCssClass(ComponentTag tag) {
        String oldCssClass = StringUtils.defaultString(tag.getAttributes().getString("class"));
        if (!isEnabled()) {
            return oldCssClass + " button " + oldCssClass + "-disabled button-disabled";
        }
        return oldCssClass + " button";
    }

    @Override
    protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
        if ("button".equals(openTag.getName())) {
            replaceComponentTagBody(markupStream, openTag, getHtml());
        } else {
            replaceComponentTagBody(markupStream, openTag, getModelObjectAsString());
        }

        if (!wasOpenCloseTag) {
            markupStream.skipRawMarkup();
            if (!markupStream.get().closes(openTag)) {
                throw new MarkupException("close tag not found for tag: " + openTag.toString() +
                        ". Component: " + toString());
            }
        }
    }

    @Override
    protected void onError(AjaxRequestTarget target, Form form) {
        super.onError(target, form);
        FeedbackUtils.refreshFeedback(target);
    }

    private String getHtml() {
        return "<div class='button-center'><div class='button-left'><div class='button-right'>"
                + getModelObjectAsString()
                + "</div></div></div>";
    }
}
