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
package org.artifactory.webapp.wicket.component.panel.titled;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author Yoav Aharoni
 */
public abstract class TitledPanel extends Panel {
    protected static final String TITLE_KEY = "panel.title";

    private Label titleLabel;

    public TitledPanel(String id) {
        super(id);
        init();
    }

    public TitledPanel(String id, IModel iModel) {
        super(id, iModel);
        init();
    }

    private void init() {
        setOutputMarkupId(true);
        titleLabel = new Label("title", new AbstractReadOnlyModel() {
            public Object getObject() {
                return getTitle();
            }
        });
        add(titleLabel);
        final AttributeAppender classAttributeAppender =
                new AttributeAppender("class", new Model("win_wrapper"), " ");
        add(classAttributeAppender);
    }

    public void hideTitle() {
        titleLabel.setVisible(false);
    }

    public String getTitle() {
        return getLocalizer().getString(TITLE_KEY, this);
    }
}
