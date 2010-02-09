package org.artifactory.webapp.wicket.common.component.panel.feedback.aggregated;

import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.artifactory.webapp.wicket.common.behavior.JavascriptEvent;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackMessagesPanel;

/**
 * @author Yoav Aharoni
 */
public class AggregateFeedbackPanel extends FeedbackMessagesPanel {
    public AggregateFeedbackPanel(String id) {
        super(id);

        add(HeaderContributor.forJavaScript(AggregateFeedbackPanel.class, "AggregateFeedbackPanel.js"));
        add(new JavascriptEvent("onclear", new ScriptModel("onClear")));
        add(new JavascriptEvent("onshow", new ScriptModel("onShow")));
    }

    private class ScriptModel extends AbstractReadOnlyModel {
        private String function;

        private ScriptModel(String function) {
            this.function = function;
        }

        @Override
        public Object getObject() {
            return "AggregateFeedbackPanel." + function + "('" + getMarkupId() + "')";
        }
    }
}
