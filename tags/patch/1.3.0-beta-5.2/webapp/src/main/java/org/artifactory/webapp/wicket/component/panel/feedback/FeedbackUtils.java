package org.artifactory.webapp.wicket.component.panel.feedback;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

/**
 * @author Yoav Aharoni
 */
public class FeedbackUtils {
    public static void refreshFeedback(final AjaxRequestTarget target) {
        getPage().visitChildren(FeedbackPanel.class, new Component.IVisitor() {
            public Object component(Component component) {
                target.addComponent(component);
                return CONTINUE_TRAVERSAL;
            }
        });
    }

    public void clearFeedback() {
        Session.get().getFeedbackMessages().clear();
    }

    private static Page getPage() {
        return RequestCycle.get().getResponsePage();
    }
}
