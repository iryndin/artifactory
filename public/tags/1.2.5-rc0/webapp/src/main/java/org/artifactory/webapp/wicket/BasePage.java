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

import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.Model;
import org.artifactory.keyval.KeyVals;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.webapp.wicket.home.HomePage;

public class BasePage extends WebPage implements IAjaxIndicatorAware {
    public BasePage() {
        setVersioned(false);
        //Allow children to override the document base
        String documentBaseUri = getDocumentBaseUri();
        WebComponent documentBaseConfig = new WebComponent("docBase");
        if (documentBaseUri != null) {
            //Don't add empty docbase href - hurts IE
            documentBaseConfig.add(new AttributeAppender("href", new Model(documentBaseUri), " "));
        }
        add(documentBaseConfig);
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
        ArtifactoryContext context = ContextHelper.get();
        KeyVals keyval = context.getKeyVal();
        String versionStr = "Artifactory " + keyval.getVersion() +
                " (rev. " + keyval.getRevision() + ")";
        add(new Label("version", new Model(versionStr)));
    }

    public final String getAjaxIndicatorMarkupId() {
        return "ajaxIndicator";
    }

    /**
     * To be overriden by inheritors
     *
     * @return
     */
    public String getDocumentBaseUri() {
        return null;
    }
}
