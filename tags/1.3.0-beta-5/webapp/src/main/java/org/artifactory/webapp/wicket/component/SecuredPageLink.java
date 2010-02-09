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
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authorization.IAuthorizationStrategy;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SecuredPageLink extends AjaxLink {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SecuredPageLink.class);

    private final Class<? extends Page> pageClass;

    public SecuredPageLink(final String id, final Class<? extends Page> c) {
        super(id);
        this.pageClass = c;
    }

    public boolean isEnabled() {
        IAuthorizationStrategy authorizationStrategy = getSession().getAuthorizationStrategy();
        boolean authorized = authorizationStrategy.isInstantiationAuthorized(pageClass);
        return authorized && super.isEnabled() && isEnableAllowed();
    }

    public void onClick(AjaxRequestTarget target) {
        target.getPage().setResponsePage(pageClass);
    }
}
