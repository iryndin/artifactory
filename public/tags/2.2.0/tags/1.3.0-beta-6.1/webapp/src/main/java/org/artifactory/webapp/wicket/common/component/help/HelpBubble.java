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
package org.artifactory.webapp.wicket.common.component.help;

import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.template.PackagedTextTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yoav Aharoni
 */

public class HelpBubble extends Label {
    protected static final String TEMPLATE_FILE = "HelpBubble.html";
    protected static final String ENCODING = "utf-8";

    /**
     * Protected constructor for explicitly for a class which overrides the class and would
     * Like to supply the model independantly
     * @param id    Wicket id
     */
    protected HelpBubble(String id) {
        super(id);
        init();
    }

    public HelpBubble(String id, String helpMessage) {
        this(id, new Model(helpMessage));
    }

    public HelpBubble(String id, IModel helpModel) {
        super(id, helpModel);
        init();
    }

    private void init() {
        setEscapeModelStrings(false);
        setOutputMarkupId(true);
        addHeaderJavascript();
    }

    protected void addHeaderJavascript() {
        add(new HeaderContributor(new IHeaderContributor() {
            public void renderHead(IHeaderResponse response) {
                response.renderJavascript("dojo.require(\"dijit.Tooltip\");", "dijit.Tooltip");
            }
        }));
    }

    @Override
    protected void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
        replaceComponentTagBody(markupStream, openTag, getHtml());
    }

    protected String getHtml() {
        PackagedTextTemplate template =
                new PackagedTextTemplate(HelpBubble.class, TEMPLATE_FILE, ENCODING);
        Map<String, String> variables = new HashMap<String, String>();
        variables.put("message", getModelObjectAsString().replaceAll("\n", "<br/>"));
        variables.put("id", getMarkupId());
        template.interpolate(variables);
        return template.getString();
    }

}
