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
package org.artifactory.webapp.wicket.widget;

import org.apache.log4j.Logger;
import wicket.Page;
import wicket.authorization.IAuthorizationStrategy;
import wicket.markup.html.link.IPageLink;
import wicket.markup.html.link.PageLink;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SecuredPageLink extends PageLink {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SecuredPageLink.class);

    private final Class<? extends Page> pageClass;

    @SuppressWarnings({"unchecked"})
    public SecuredPageLink(final String id, final Class c) {
        super(id, c);
        this.pageClass = c;
    }

    public SecuredPageLink(final String id, final Page page) {
        super(id, page);
        this.pageClass = page.getClass();
    }

    @SuppressWarnings({"unchecked"})
    public SecuredPageLink(final String id, final IPageLink pageLink) {
        super(id, pageLink);
        this.pageClass = pageLink.getPageIdentity();
    }

    public boolean isEnabled() {
        IAuthorizationStrategy authorizationStrategy = getSession().getAuthorizationStrategy();
        boolean authorized = authorizationStrategy.isInstantiationAuthorized(pageClass);
        return authorized && super.isEnabled() && isEnableAllowed();
    }
}
