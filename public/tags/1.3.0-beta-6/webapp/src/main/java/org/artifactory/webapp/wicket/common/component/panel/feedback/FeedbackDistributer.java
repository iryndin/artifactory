package org.artifactory.webapp.wicket.common.component.panel.feedback;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;

import java.util.Collections;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
@SuppressWarnings({"BooleanMethodNameMustStartWithQuestion"})
public class FeedbackDistributer extends Panel implements IFeedback {
    private FeedbackMessagesPanel defaultFeedbackPanel;

    public FeedbackDistributer(String id) {
        super(id);

        setModel(newFeedbackMessagesModel());
        setOutputMarkupId(true);

        add(HeaderContributor.forJavaScript(getClass(), "FeedbackDistributer.js"));

        Label script = new Label("script", new ScriptModel());
        script.setEscapeModelStrings(false);
        script.setVersioned(false);
        add(script);
    }

    public FeedbackMessagesPanel getDefaultFeedbackPanel() {
        return defaultFeedbackPanel;
    }

    public void setDefaultFeedbackPanel(FeedbackMessagesPanel defaultFeedbackPanel) {
        this.defaultFeedbackPanel = defaultFeedbackPanel;
    }

    /**
     * @see Component#isVersioned()
     */
    @SuppressWarnings({"RefusedBequest"})
    @Override
    public boolean isVersioned() {
        return false;
    }

    public final FeedbackMessagesModel getFeedbackMessagesModel() {
        return (FeedbackMessagesModel) getModel();
    }

    public void updateFeedback() {
        // Force model to load
        getModelObject();
    }

    public boolean anyErrorMessage() {
        return anyMessage(FeedbackMessage.ERROR);
    }

    public boolean anyMessage() {
        return anyMessage(FeedbackMessage.UNDEFINED);
    }

    public boolean anyMessage(int level) {
        List<FeedbackMessage> msgs = getCurrentMessages();

        for (FeedbackMessage msg : msgs) {
            if (msg.isLevel(level)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings({"unchecked"})
    protected final List<FeedbackMessage> getCurrentMessages() {
        List<FeedbackMessage> messages = (List<FeedbackMessage>) getModelObject();
        return Collections.unmodifiableList(messages);
    }

    protected FeedbackMessagesModel newFeedbackMessagesModel() {
        return new FeedbackMessagesModel(this);
    }

    private class ScriptModel extends AbstractReadOnlyModel {
        @Override
        public Object getObject() {
            StringBuffer initScript = new StringBuffer();

            // init
            String defaultFeedbackId = defaultFeedbackPanel == null
                    ? "" : defaultFeedbackPanel.getMarkupId();

            initScript
                    .append("(function() {\n")
                    .append("var fd = FeedbackDistributer;\n")
                    .append("fd.init('")
                    .append(defaultFeedbackId)
                    .append("');\n");

            // add messages
            for (FeedbackMessage message : getCurrentMessages()) {
                message.markRendered();
                initScript
                        .append("fd.addMessage('")
                        .append(getMarkupIdFor(message.getReporter()))
                        .append("', '")
                        .append(message.getLevelAsString())
                        .append("', ")
                        .append(asJsStringParam(message.getMessage()))
                        .append(");\n");
            }

            initScript.append("fd.showMessages();\n");
            initScript.append("})();\n");
            return initScript;
        }

        private String getMarkupIdFor(Component component) {
            Component current = component;

            // find first parent with markup id
            while (current != null) {
                if (current.getOutputMarkupId()) {
                    return current.getMarkupId();
                }

                current = current.getParent();
            }

            return "";
        }
    }

    public static String asJsStringParam(Object param) {
        return '\'' + param.toString()
                .replaceAll("\\\\", "\\\\\\\\")
                .replaceAll("'", "\\\\'")
                .replaceAll("\n", "<br/>")
                .replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
                .replaceAll("\r", "") + '\'';
    }
}
