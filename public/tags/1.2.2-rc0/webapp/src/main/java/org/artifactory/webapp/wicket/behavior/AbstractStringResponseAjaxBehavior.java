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
package org.artifactory.webapp.wicket.behavior;

import org.apache.log4j.Logger;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.target.basic.StringRequestTarget;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class AbstractStringResponseAjaxBehavior extends AbstractAjaxBehavior {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(AbstractStringResponseAjaxBehavior.class);

    private static final long serialVersionUID = 1L;

    protected void onBind() {
        getComponent().setOutputMarkupId(true);
    }

    public final void onRequest() {
        boolean isPageVersioned = true;
        Page page = getComponent().getPage();
        try {
            isPageVersioned = page.isVersioned();
            page.setVersioned(false);
            String response = getResponse();
            RequestCycle cycle = RequestCycle.get();
            boolean redirect = cycle.getRedirect();
            IRequestTarget target = new StringRequestTarget(response);
            cycle.setRequestTarget(target);
        } finally {
            page.setVersioned(isPageVersioned);
        }
    }

    /**
     * Return a string response or a redirect-to url
     *
     * @return
     */
    protected abstract String getResponse();
}
