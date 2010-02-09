/*
 * This file is part of Artifactory.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.common.wicket.component.help;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.artifactory.common.wicket.behavior.tooltip.TooltipBehavior;
import org.artifactory.common.wicket.contributor.ResourcePackage;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yoav Aharoni
 */

public class HelpBubble extends Label {
    protected static final String TEMPLATE_FILE = "HelpBubble.html";
    protected static final String ENCODING = "utf-8";

    /**
     * Protected constructor for explicitly for a class which overrides the class and would Like to supply the model
     * independantly
     *
     * @param id Wicket id
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
        add(ResourcePackage.forJavaScript(TooltipBehavior.class));
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
        variables.put("enabled", String.valueOf(isEnabled()));
        variables.put("id", getMarkupId());
        template.interpolate(variables);
        return template.getString();
    }

}
