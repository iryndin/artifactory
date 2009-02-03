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

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.artifactory.webapp.wicket.common.Titled;
import org.artifactory.webapp.wicket.common.behavior.defaultbutton.DefaultButtonBehavior;

/**
 * This panel behaves like a TitledPanel with additional buttons panel at the bottom right,
 * outside of the grey border.
 *
 * @author Yossi Shaul
 */
public abstract class TitledActionPanel extends Panel implements Titled {
    protected static final String TITLE_KEY = "panel.title";

    private RepeatingView buttonsContainer;

    protected TitledActionPanel(String id) {
        super(id);
        init();
    }

    protected TitledActionPanel(String id, IModel iModel) {
        super(id, iModel);
        init();
    }

    private void init() {
        setOutputMarkupId(true);
        add(new TitleLabel(this));
        buttonsContainer = new RepeatingView("myButton");
        add(buttonsContainer);
    }

    public String getTitle() {
        return "";
    }

    /**
     * Adds a button to the buttons list and marks the button as the default.
     *
     * @param button The button to add and mark as default.
     */
    protected void addDefaultButton(Button button) {
        addButton((Component) button);
        button.getForm().add(new DefaultButtonBehavior(button));
    }

    /**
     * Adds a button to the buttons list on the bottom left of the panel.
     * The buttons will be displayed in the order they were added.
     *
     * @param button The button to add.
     */
    protected void addButton(Button button) {
        addButton((Component) button);
    }

    /**
     * Adds a button to the buttons list on the bottom left of the panel.
     * The buttons will be displayed in the order they were added.
     *
     * @param button The button to add.
     */
    protected void addButton(AbstractLink button) {
        addButton((Component) button);
    }

    private void addButton(Component button) {
        buttonsContainer.add(button);
    }
}