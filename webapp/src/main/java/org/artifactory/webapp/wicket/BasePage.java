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

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RequestCycle;
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
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.VersionInfo;
import org.artifactory.webapp.wicket.home.HomePage;

import java.util.HashMap;
import java.util.Map;

public class BasePage extends WebPage implements IAjaxIndicatorAware {

    @SpringBean
    private CentralConfigService centralConfig;

    private Map<String, Integer> ids = null;

    public BasePage() {
        setVersioned(false);
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
        WebRequestCycle webRequestCycle = (WebRequestCycle) getRequestCycle();
        WebRequest request = webRequestCycle.getWebRequest();
        String src = request.getRelativePathPrefixToContextRoot() + "js130beta3/dojo/dojo.js";
        djConfig.add(new AttributeAppender("src", new Model(src), " "));
        add(djConfig);
        add(new BookmarkablePageLink("artifactoryLink", HomePage.class));
        // TODO: Need to cache this object
        VersionInfo versionInfo = centralConfig.getVersionInfo();
        String versionStr = "Artifactory " + versionInfo.getVersion() +
                " (rev. " + versionInfo.getRevision() + ")";
        add(new Label("version", new Model(versionStr)));
    }

    public final String getAjaxIndicatorMarkupId() {
        return "ajaxIndicator";
    }

    public Map<String, Integer> getIds() {
        if (ids == null) {
            ids = new HashMap<String, Integer>();
        }
        return ids;
    }

    @Override
    protected void componentChanged(Component component, MarkupContainer parent) {
        ArtifactoryRequestCycle cycle = (ArtifactoryRequestCycle) RequestCycle.get();
        if (cycle.getAjaxData() != null) {
            cycle.getAjaxData().componentChanged(component, parent);
        }
    }
}
