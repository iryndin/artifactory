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

package org.artifactory.common.wicket.component.links;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.CancelEventIfNoAjaxDecorator;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.IAjaxLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmittingComponent;
import org.apache.wicket.model.IModel;
import org.artifactory.common.wicket.util.AjaxUtils;

/**
 * @author Yoav Aharoni
 */
public abstract class TitledAjaxSubmitLink extends BaseTitledLink implements IAjaxLink, IFormSubmittingComponent {
    private Form form;

    protected TitledAjaxSubmitLink(String id) {
        this(id, (Form) null);
    }

    protected TitledAjaxSubmitLink(String id, IModel titleModel) {
        this(id, titleModel, null);
    }


    protected TitledAjaxSubmitLink(String id, String title) {
        this(id, title, null);
    }

    protected TitledAjaxSubmitLink(String id, Form form) {
        super(id);
        this.form = form;
        init();
    }

    protected TitledAjaxSubmitLink(String id, IModel titleModel, Form form) {
        super(id, titleModel);
        this.form = form;
        init();
    }

    protected TitledAjaxSubmitLink(String id, String title, Form form) {
        super(id, title);
        this.form = form;
        init();
    }

    protected abstract void onSubmit(AjaxRequestTarget target, Form form);

    protected void onError(AjaxRequestTarget target) {
        AjaxUtils.refreshFeedback(target);
    }

    protected IAjaxCallDecorator getAjaxCallDecorator() {
        return null;
    }

    public boolean getDefaultFormProcessing() {
        return true;
    }

    public final Form getForm() {
        if (form == null) {
            // try to find form in the hierarchy of owning component
            form = (Form) this.findParent(Form.class);
            if (form == null) {
                throw new IllegalStateException(
                        "form was not specified in the constructor and cannot be found in the hierarchy of the TitledAjaxSubmitLink");
            }
        }
        return form;
    }

    public final String getInputName() {
        return getId();
    }

    public final void onSubmit() {
    }

    public final void onClick(AjaxRequestTarget target) {
        onSubmit(target, getForm());
    }

    private void init() {
        add(new AjaxFormSubmitBehavior(form, "onclick") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                TitledAjaxSubmitLink.this.onSubmit(target, getForm());
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                TitledAjaxSubmitLink.this.onError(target);
            }

            @SuppressWarnings({"RefusedBequest"})
            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new CancelEventIfNoAjaxDecorator(TitledAjaxSubmitLink.this.getAjaxCallDecorator());
            }
        });
    }
}
