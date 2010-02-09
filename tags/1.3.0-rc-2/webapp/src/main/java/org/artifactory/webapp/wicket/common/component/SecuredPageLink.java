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
package org.artifactory.webapp.wicket.common.component;

import org.apache.wicket.Page;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SecuredPageLink extends BookmarkablePageLink {

    private final Class<? extends Page> pageClass;

    public SecuredPageLink(final String id, String caption, final Class<? extends Page> pageClass) {
        super(id, pageClass);
        if (caption != null) {
            setModel(new Model(caption));
        }
        this.pageClass = pageClass;
    }

    @Override
    public boolean isEnabled() {
        IAuthorizationStrategy authorizationStrategy = getSession().getAuthorizationStrategy();
        boolean authorized = authorizationStrategy.isInstantiationAuthorized(pageClass);
        return authorized && super.isEnabled() && isEnableAllowed();
    }
}
