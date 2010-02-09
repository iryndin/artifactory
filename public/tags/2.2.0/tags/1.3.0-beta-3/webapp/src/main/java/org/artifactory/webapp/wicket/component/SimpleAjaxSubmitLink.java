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
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Form;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SimpleAjaxSubmitLink extends AjaxSubmitLink {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SimpleButton.class);

    public SimpleAjaxSubmitLink(String id, Form form) {
        super(id, form);
    }

    protected void onSubmit(final AjaxRequestTarget target, Form form) {
        addTargets(target, form);
    }

    @Override
    protected void onError(AjaxRequestTarget target, Form form) {
        addTargets(target, form);
    }

    private static void addTargets(final AjaxRequestTarget target, Form form) {
        //Add the form and all feedback panels
        Page page = form.getPage();
        page.visitChildren(IFeedback.class, new IVisitor() {
            public Object component(Component component) {
                target.addComponent(component);
                return IVisitor.CONTINUE_TRAVERSAL;
            }

        });
        target.addComponent(form);
    }
}