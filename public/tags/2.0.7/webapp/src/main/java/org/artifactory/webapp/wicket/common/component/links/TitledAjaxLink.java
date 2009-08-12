package org.artifactory.webapp.wicket.common.component.links;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.CancelEventIfNoAjaxDecorator;
import org.apache.wicket.ajax.markup.html.IAjaxLink;
import org.apache.wicket.model.IModel;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;

/**
 * AjaxButton can get it's text from resourceKey or from markup
 *
 * @author Yoav Aharoni
 */
public abstract class TitledAjaxLink extends BaseTitledLink implements IAjaxLink {

    protected TitledAjaxLink(String id) {
        super(id);
    }

    protected TitledAjaxLink(String id, IModel titleModel) {
        super(id, titleModel);
    }

    protected TitledAjaxLink(String id, String title) {
        super(id, title);
    }

    {
        add(new AjaxEventBehavior("onclick") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onClick(target);
                FeedbackUtils.refreshFeedback(target);
            }

            @SuppressWarnings({"RefusedBequest"})
            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new CancelEventIfNoAjaxDecorator(TitledAjaxLink.this.getAjaxCallDecorator());
            }

        });
    }

    protected IAjaxCallDecorator getAjaxCallDecorator() {
        return null;
    }
}
