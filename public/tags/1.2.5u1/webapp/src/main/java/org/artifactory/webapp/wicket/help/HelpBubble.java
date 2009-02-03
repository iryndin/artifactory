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
package org.artifactory.webapp.wicket.help;

import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.template.PackagedTextTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yoav Aharoni
 */

// todo >> yoava: replace help bubble with js
public class HelpBubble extends Label {
    protected static final String TEMPLATE_FILE = "HelpBubble.html";
    protected static final String ENCODING = "UTF-8";

    public HelpBubble(String id, final String helpMessage) {
        super(id);
        setModel(new Model(getHtml(helpMessage)));
        setEscapeModelStrings(false);
        setRenderBodyOnly(true);
        addHeaderJavascript();
    }

    protected void addHeaderJavascript() {
        add(new HeaderContributor(new IHeaderContributor() {
            public void renderHead(IHeaderResponse response) {
                response.renderJavascript("dojo.require(\"dojo.widget.Tooltip\");",
                        "dojo_widget_Tooltip");
            }
        }));
    }

    protected String getHtml(final String helpMessage) {
        PackagedTextTemplate template =
                new PackagedTextTemplate(HelpBubble.class, TEMPLATE_FILE, ENCODING);
        Map<String, String> variables = new HashMap<String, String>();
        variables.put("message", helpMessage);
        variables.put("id", getId());
        template.interpolate(variables);
        return template.getString();
    }

}
