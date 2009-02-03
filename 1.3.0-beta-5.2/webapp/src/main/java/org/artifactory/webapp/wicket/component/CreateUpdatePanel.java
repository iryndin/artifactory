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
package org.artifactory.webapp.wicket.component;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.artifactory.webapp.wicket.component.panel.titled.TitledPanel;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class CreateUpdatePanel<E> extends TitledPanel {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private static final Logger LOGGER = Logger.getLogger(CreateUpdatePanel.class);


    public enum CreateUpdateAction {
        CREATE, UPDATE
    }

    protected E entity;
    protected CreateUpdateAction action;
    protected Form form;

    public CreateUpdatePanel(CreateUpdateAction action, E entity) {
        super("createUpdate");
        this.entity = entity;
        this.action = action;
        CompoundPropertyModel model = new CompoundPropertyModel(entity);
        this.form = new Form("form", model);
        setOutputMarkupId(true);
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        //Notify the form
        CompoundPropertyModel model = new CompoundPropertyModel(entity);
        form.setModel(model);
    }

    @Override
    public String getTitle() {
        return getLocalizer().getString(TITLE_KEY + "." + action.name().toLowerCase(), this);
    }

    public void replaceWith(AjaxRequestTarget target, CreateUpdatePanel<E> panel) {
        replaceWith(panel);
        target.addComponent(panel);
    }

    protected boolean isCreate() {
        return action.equals(CreateUpdateAction.CREATE);
    }
}
