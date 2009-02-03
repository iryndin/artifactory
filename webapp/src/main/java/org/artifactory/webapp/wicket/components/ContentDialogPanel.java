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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ContentDialogPanel extends Panel {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ContentDialogPanel.class);

    private final Label contentText = new Label("contentText");

    public ContentDialogPanel(String id) {
        super(id);
        setOutputMarkupId(false);
        contentText.setOutputMarkupId(true);
        add(contentText);
    }

    public Label getContentText() {
        return contentText;
    }

    public String getContent() {
        return (String) contentText.getModelObject();
    }

    public void ajaxUpdate(String content, AjaxRequestTarget target) {
        contentText.setModel(new Model(content));
        target.addComponent(contentText);
        target.appendJavascript("ContentDialog.show();");
    }
}
