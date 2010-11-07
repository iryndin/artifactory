/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.common.wicket.component.panel;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.WicketTag;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.parser.filter.WicketTagIdentifier;
import org.apache.wicket.markup.resolver.IComponentResolver;
import org.apache.wicket.model.IModel;

/**
 * @author Yoav Aharoni
 */
public class PanelWithBody extends Panel implements IComponentResolver {
    private transient MarkupStream bodyMarkup;
    private transient ComponentTag openTag;

    public PanelWithBody(String id) {
        super(id);
    }

    public PanelWithBody(String id, IModel model) {
        super(id, model);
    }

    public boolean resolve(MarkupContainer container, MarkupStream markupStream, ComponentTag tag) {
        // handle <wicket:body/> tag, render body
        if (tag instanceof WicketTag && ((WicketTag) tag).isBodyTag()) {
            setMarkupStream(bodyMarkup);
            renderComponentTagBody(bodyMarkup, openTag);
            setMarkupStream(markupStream);
            markupStream.next();
            return true;
        }

        return false;
    }

    @Override
    protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
        this.bodyMarkup = markupStream;
        this.openTag = openTag;
        super.onComponentTagBody(markupStream, openTag);
        this.bodyMarkup = null;
        this.openTag = null;
    }

    static {
        WicketTagIdentifier.registerWellKnownTagName("body");
    }
}
