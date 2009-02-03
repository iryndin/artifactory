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
package org.artifactory.webapp.wicket.components;

import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class AjaxDeleteRow<T> extends WebMarkupContainer {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(AjaxDeleteRow.class);

    private T toBeDeletedObject;

    @SuppressWarnings({"unchecked"})
    public AjaxDeleteRow(String id, IModel model, final Component listener) {
        super(id);
        toBeDeletedObject = (T) model.getObject();
        add(new AjaxEventBehavior("onClick") {
            protected void onEvent(final AjaxRequestTarget target) {
                doDelete();
                onDeleted(target, listener);
            }

            @SuppressWarnings({"UnnecessaryLocalVariable"})
            @Override
            protected CharSequence getCallbackScript() {
                CharSequence orig = super.getCallbackScript();
                String callbackScript =
                        "if (confirm('" + getConfirmationQuestion() + "')) {" +
                                orig + "} else { return false; }";
                return callbackScript;
            }
        });
    }

    public T getToBeDeletedObject() {
        return toBeDeletedObject;
    }

    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        tag.setName("img");
        tag.put("src", "../images/delete.png");
        tag.put("alt", "Delete");
        tag.put("style", "cursor:pointer;");
    }

    @Override
    protected void onComponentTagBody(
            final MarkupStream markupStream, final ComponentTag openTag) {
        replaceComponentTagBody(markupStream, openTag, "");
    }

    protected abstract void doDelete();

    protected abstract void onDeleted(AjaxRequestTarget target, Component listener);

    protected abstract String getConfirmationQuestion();
}
