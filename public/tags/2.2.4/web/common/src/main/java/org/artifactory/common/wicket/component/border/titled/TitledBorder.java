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

package org.artifactory.common.wicket.component.border.titled;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.model.IModel;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.PlaceHolder;
import org.artifactory.common.wicket.component.panel.titled.TitleLabel;
import org.artifactory.common.wicket.model.Titled;

/**
 * @author Yoav Aharoni
 */
public class TitledBorder extends Border implements Titled {
    private static final String[] CSS_CLASS =
            {"-top", "-left", "-right", "-top-left", "-top-right", "-bottom", "-bottom-left", "-bottom-right"};

    private String cssPrefix;

    public TitledBorder(String id) {
        this(id, "border");
    }

    public TitledBorder(String id, IModel model) {
        this(id, model, "border");
    }

    public TitledBorder(String id, String cssPrefix) {
        this(id, null, cssPrefix);
    }

    public TitledBorder(String id, IModel model, String cssPrefix) {
        super(id, model);
        this.cssPrefix = cssPrefix;
        init();
    }

    private void init() {
        setOutputMarkupId(true);
        add(new CssClass(cssPrefix + "-wrapper"));

        // building the borders
        MarkupContainer container = addBorders();

        // container - is a last border
        WebMarkupContainer title = new WebMarkupContainer("title") {
            @Override
            public boolean isVisible() {
                return super.isVisible() && StringUtils.isNotEmpty(getTitle());
            }
        };
        title.add(new CssClass(cssPrefix + "-title"));
        container.add(title);

        TitleLabel titleLabel = new TitleLabel(this);
        title.add(titleLabel);
        title.add(newToolbar("tool"));
    }

    private MarkupContainer addBorders() {
        MarkupContainer container = this;

        for (String div : CSS_CLASS) {
            // creating new container
            WebMarkupContainer border = new WebMarkupContainer("border");

            // modifiying the the class-attribute
            border.add(new SimpleAttributeModifier("class", getCssPrefix() + div));

            // adding to the current container
            container.add(border);

            // replacing the container variable with wicketContainer
            container = border;
        }

        return container;
    }

    protected String getCssPrefix() {
        return cssPrefix;
    }

    public String getTitle() {
        return getString(getId(), null, "");
    }

    protected Component newToolbar(String id) {
        return new PlaceHolder(id);
    }
}
