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
package org.artifactory.webapp.wicket;

import org.artifactory.keyval.KeyVals;
import org.artifactory.repo.CentralConfig;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextUtils;
import org.artifactory.webapp.wicket.home.HomePage;
import wicket.markup.ComponentTag;
import wicket.markup.MarkupStream;
import wicket.markup.html.WebComponent;
import wicket.markup.html.WebPage;
import wicket.markup.html.basic.Label;
import wicket.markup.html.link.BookmarkablePageLink;
import wicket.markup.parser.XmlTag;
import wicket.model.Model;

public class BasePage extends WebPage {
    public BasePage() {
        //Write the dojo debug configuration based on wicket configuration
        boolean debug = getApplication().getDebugSettings().isAjaxDebugModeEnabled();
        final String configJs = "var djConfig = { isDebug: " + debug +
                ", debugAtAllCosts: false, excludeNamespace: [\"wicket\"]};";
        WebComponent djConfig = new WebComponent("djConfig", new Model(configJs)) {
            @Override
            protected void onComponentTag(final ComponentTag tag) {
                if (tag.isOpenClose()) {
                    tag.setType(XmlTag.OPEN);
                }
                super.onComponentTag(tag);
            }

            @Override
            protected void onComponentTagBody(
                    final MarkupStream markupStream, final ComponentTag openTag) {
                getResponse().write(configJs);
            }
        };
        add(djConfig);
        add(new BookmarkablePageLink("artifactory_link", HomePage.class));
        KeyVals keyval = getContext().getKeyVal();
        String versionStr = "Artifactory " + keyval.getVersion() +
                " (rev. " + keyval.getRevision() + ")";
        add(new Label("version", new Model(versionStr)));
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public ArtifactoryContext getContext() {
        ArtifactoryContext context = ContextUtils.getArtifactoryContext();
        return context;
    }

    public CentralConfig getCc() {
        ArtifactoryContext context = getContext();
        return context.getCentralConfig();
    }
}
