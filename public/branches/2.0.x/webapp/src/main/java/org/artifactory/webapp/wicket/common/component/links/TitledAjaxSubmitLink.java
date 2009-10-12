package org.artifactory.webapp.wicket.common.component.links;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.IAjaxLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmittingComponent;
import org.apache.wicket.model.IModel;

/**
 * @author Yoav Aharoni
 */
public abstract class TitledAjaxSubmitLink extends BaseTitledLink implements IAjaxLink, IFormSubmittingComponent {

    protected TitledAjaxSubmitLink(String id) {
        super(id);
    }

    protected TitledAjaxSubmitLink(String id, IModel titleModel) {
        super(id, titleModel);
    }

    protected TitledAjaxSubmitLink(String id, String title) {
        super(id, title);
    }

    {
        add(new AjaxFormSubmitBehavior("onclick") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                TitledAjaxSubmitLink.this.onSubmit(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                TitledAjaxSubmitLink.this.onError(target);
            }

            @Override
            protected CharSequence getEventHandler() {
                return super.getEventHandler() + " return false;";
            }
        });
    }

    protected abstract void onSubmit(AjaxRequestTarget target);

    public boolean getDefaultFormProcessing() {
        return true;
    }

    public Form getForm() {
        return null;
    }

    public String getInputName() {
        return getId();
    }

    public void onSubmit() {
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void onError(AjaxRequestTarget target) {
    }

    public final void onClick(AjaxRequestTarget target) {
        onSubmit(target);
    }
}
