/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.common.wicket.component.modal.panel.bordered;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.artifactory.common.wicket.behavior.CssClass;

import static java.lang.String.format;

/**
 * @author Yoav Aharoni
 */
public class CodeModalPanel extends BorderedModalPanel implements IHeaderContributor {
    public CodeModalPanel(Component content) {
        super(content);
        content.add(new CssClass("modal-code"));
        content.setOutputMarkupId(true);
    }

    public void renderHead(IHeaderResponse response) {
        response.renderJavascriptReference(new ResourceReference(CodeModalPanel.class, "CodeModalPanel.js"));
    }

    @Override
    public void onShow(AjaxRequestTarget target) {
        String markupId = get("border:content").getMarkupId();
        target.appendJavascript(format("ModelCode.onShow('%s');", markupId));
    }

    @Override
    public void onClose(AjaxRequestTarget target) {
        target.appendJavascript("ModelCode.onClose();");
    }
}
