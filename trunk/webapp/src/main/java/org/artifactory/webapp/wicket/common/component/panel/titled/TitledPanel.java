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
package org.artifactory.webapp.wicket.common.component.panel.titled;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.artifactory.webapp.wicket.common.Titled;
import org.artifactory.webapp.wicket.common.behavior.CssClass;

/**
 * @author Yoav Aharoni
 */
public abstract class TitledPanel extends Panel implements Titled {
    protected static final String TITLE_KEY = "panel.title";

    protected TitledPanel(String id) {
        super(id);
        init();
    }

    protected TitledPanel(String id, IModel iModel) {
        super(id, iModel);
        init();
    }

    private void init() {
        setOutputMarkupId(true);
        add(new TitleLabel(this));
        add(new CssClass("border-wrapper"));
    }

    public String getTitle() {
        return getString(TITLE_KEY, null);
    }
}
