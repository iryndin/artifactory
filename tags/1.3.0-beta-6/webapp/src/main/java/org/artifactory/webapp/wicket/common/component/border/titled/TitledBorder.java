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
    private static final String[] BORDER_DIRECTIONS = {"-top", "-bottom", "-left", "-right", "-top-left", "-top-right", "-bottom-left", "-bottom-right"};

    private String cssClass;

    public TitledBorder(String id) {
        this(id, "border");
    }

    public TitledBorder(String id, String cssClass) {
        super(id);
        this.cssClass = cssClass;
        init();
    }


    private void init() {
        setOutputMarkupId(true);
        // building the borders
        MarkupContainer container = addBorders();
        // container - is a last border
        container.add(new TitleLabel(this));
        add(new CssClass(cssClass + "-wrapper"));
    }

    private MarkupContainer addBorders() {
        MarkupContainer container = this;

        for (String borderDirection : BORDER_DIRECTIONS) {
            // creating new container
            WebMarkupContainer wicketBorder = new WebMarkupContainer("border");
            // modifiying the the class-attribute
            wicketBorder.add(new SimpleAttributeModifier("class", getBorderCssClassName() + borderDirection));
            // adding to the current container
            container.add(wicketBorder);
            // replacing the container variable with wicketContainer
            container = wicketBorder;
        }

        return container;
    }

    protected String getBorderCssClassName() {
        return cssClass;
    }

    public String getTitle() {
        return getString(getId(), null, "");
    }
}
