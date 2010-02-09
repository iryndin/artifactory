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
import org.artifactory.webapp.wicket.panel.WindowPanel;
import wicket.Component;
import wicket.ajax.AjaxRequestTarget;
import wicket.markup.html.panel.FeedbackPanel;

import java.awt.*;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class CreateUpdatePanel<E> extends WindowPanel {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(CreateUpdatePanel.class);


    public static enum CreateUpdateAction {
        CREATE, UPDATE
    }

    protected E entity;

    protected CreateUpdateAction action;

    protected Component changeListener;

    protected FeedbackPanel feedback;

    private CreateUpdatePanel otherPanel;

    public CreateUpdatePanel(String id, CreateUpdateAction initialAction, E entity) {
        super(id);
        this.entity = entity;
        this.action = initialAction;
        setOutputMarkupId(true);
        setVersioned(false);
        //Feedback
        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);
    }

    public CreateUpdatePanel getOtherPanel() {
        return otherPanel;
    }

    public void setOtherPanel(CreateUpdatePanel otherPanel) {
        if (this.otherPanel == null) {
            this.otherPanel = otherPanel;
        } else {
            throw new IllegalArgumentException("otherPanel already set.");
        }
    }

    public void setChangeListener(Component changeListener) {
        this.changeListener = changeListener;
    }

    public void flip(AjaxRequestTarget target) {
        otherPanel.setVisible(true);
        setVisible(false);
        if (target != null) {
            target.addComponent(this.getParent());
            if (changeListener != null) {
                target.addComponent(changeListener);
            }
            target.addComponent(otherPanel.getParent());
            //Ugly hack to override a wicket bug that does not update the rendering of the newly
            //visible feedback panel, and returns an NPE when trying to render a child list item
            otherPanel.feedback.internalAttach();
            if (otherPanel.changeListener != null) {
                target.addComponent(otherPanel.changeListener);
            }
        }
    }

    public void updateModelObject(E entity, AjaxRequestTarget target) {
        modelChanging();
        this.entity = entity;
        modelChanged();
        show(target);
    }

    public void show(AjaxRequestTarget target) {
        if (!isVisible()) {
            otherPanel.flip(target);
        } else if (target != null) {
            target.addComponent(this.getParent());
            if (changeListener != null) {
                target.addComponent(changeListener);
            }
        }
    }

    protected void clearFeedback(AjaxRequestTarget target) {
        List list = ((List) feedback.getModelObject());
        if (list != null) {
            list.removeAll();
        }
        if (target != null) {
            target.addComponent(feedback);
        }
    }
}
