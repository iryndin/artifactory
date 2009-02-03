package org.artifactory.webapp.wicket.common.behavior.defaultbutton;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmittingComponent;
import org.artifactory.webapp.wicket.common.behavior.CssClass;

/**
 * @author Yoav Aharoni
 */
public class DefaultButtonBehavior extends AbstractBehavior {
    private IFormSubmittingComponent defaultButton;

    public DefaultButtonBehavior(IFormSubmittingComponent defaultButton) {
        this.defaultButton = defaultButton;
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        if (!(component instanceof Form)) {
            throw new IllegalArgumentException(DefaultButtonBehavior.class.getSimpleName()
                    + " can only be added to Form components.");
        }

        Form form = (Form) component;
        form.setDefaultButton(defaultButton);
        ((Component) defaultButton).add(new CssClass("default-button"));

    }
}
