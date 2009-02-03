package org.artifactory.webapp.wicket.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;

/**
 * The cancel button is used to clear the input form data.
 *
 * @author Yossi Shaul
 */
public class CancelButton extends SimpleButton {

    public CancelButton(Form form) {
        this("cancel", form);
    }

    public CancelButton(String id, Form form) {
        this(id, form, "Cancel");
    }

    public CancelButton(String id, Form form, String caption) {
        super(id, form, caption);
        setDefaultFormProcessing(false);
    }

    protected void onSubmit(AjaxRequestTarget target, Form form) {
        form.clearInput();
    }

}
