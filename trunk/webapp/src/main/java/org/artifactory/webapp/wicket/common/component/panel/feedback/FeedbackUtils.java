package org.artifactory.webapp.wicket.common.component.panel.feedback;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;

/**
 * @author Yoav Aharoni
 */
public class FeedbackUtils {
    public static void refreshFeedback(final AjaxRequestTarget target) {
        Page page = getPage();
        if (page == null || target == null) {
            return;
        }
        page.visitChildren(IFeedback.class, new Component.IVisitor() {
            public Object component(Component component) {
                if (component.getOutputMarkupId()) {
                    target.addComponent(component);
                }
                return CONTINUE_TRAVERSAL;
            }
        });
    }

    private static Page getPage() {
        return RequestCycle.get().getResponsePage();
    }
}
