package org.artifactory.webapp.wicket.common.component.border.titled;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.border.Border;
import org.artifactory.webapp.wicket.common.Titled;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitleLabel;

/**
 * @author Yoav Aharoni
 */
public class TitledBorder extends Border implements Titled {
    private static final String[] CSS_CLASS = {"-top", "-bottom", "-left", "-right", "-top-left", "-top-right", "-bottom-left", "-bottom-right"};

    private String cssPrefix;

    public TitledBorder(String id) {
        this(id, "border");
    }

    public TitledBorder(String id, String cssPrefix) {
        super(id);
        this.cssPrefix = cssPrefix;
        init();
    }

    private void init() {
        setOutputMarkupId(true);
        add(new CssClass(cssPrefix + "-wrapper"));

        // building the borders
        MarkupContainer container = addBorders();

        // container - is a last border
        TitleLabel titleLabel = new TitleLabel(this);
        titleLabel.add(new CssClass(cssPrefix + "-title"));
        container.add(titleLabel);
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
}
