package org.artifactory.webapp.wicket.behavior;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;

/**
 * @author Yoav Aharoni
 */
public class SubmitWithButtonBehavior extends AbstractBehavior {
    private Component defaultButton;

    public SubmitWithButtonBehavior(Component defaultButton) {
        this.defaultButton = defaultButton;
    }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        tag.put("onsubmit", "return defaultSubmitButton('" + defaultButton.getMarkupId() + "');");
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        component.add(TextTemplateHeaderContributor.forJavaScript(SubmitWithButtonBehavior.class, "SubmitWithButtonBehavior.js", new Model()));
    }
}
