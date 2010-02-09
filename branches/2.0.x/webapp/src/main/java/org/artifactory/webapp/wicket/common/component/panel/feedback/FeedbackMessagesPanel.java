package org.artifactory.webapp.wicket.common.component.panel.feedback;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;

/**
 * @author Yoav Aharoni
 */
public class FeedbackMessagesPanel extends Label {

    public FeedbackMessagesPanel(String id) {
        super(id, "");
        setOutputMarkupId(true);
    }

    public FeedbackMessagesPanel addMessagesSource(Component component) {
        component.add(new AttributeModifier("feedbackId", true, new MarkupIdModel()));
        return this;
    }

    private class MarkupIdModel extends AbstractReadOnlyModel {
        @Override
        public Object getObject() {
            return getMarkupId();
        }
    }
}
