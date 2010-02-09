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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitButton;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.Form;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class DojoButton extends AjaxSubmitButton {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(DojoButton.class);

    private String caption;

    public DojoButton(String id, Form form) {
        super(id, form);
    }

    public DojoButton(String id, Form form, String caption) {
        super(id, form);
        this.caption = caption;
        setOutputMarkupId(true);
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    protected void onSubmit(AjaxRequestTarget target, Form form) {
        String id = getId();
        String call = "dojo.widget.manager.getWidgetById('" + id + "').postCreate()";
        target.appendJavascript(call);
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        //tag.put("dojoType", "Button");
        tag.put("caption", caption);
        tag.put("value", caption);
        //tag.put("widgetId", getId());
    }
}
