package org.artifactory.common.wicket.ajax;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import static org.artifactory.common.wicket.util.JavaScriptUtils.jsFunctionCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Eli Givoni
 */
public abstract class ConfirmAjaxBehavior extends AjaxFormComponentUpdatingBehavior {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(ConfirmAjaxBehavior.class);

    private IModel messageModel;
    private boolean confirmEnabled = true;

    protected ConfirmAjaxBehavior(String event, String message) {
        this(event, new Model(message));
    }

    protected ConfirmAjaxBehavior(String event, IModel messageModel) {
        super(event);
        this.messageModel = messageModel;
    }

    @Override
    protected final void onUpdate(AjaxRequestTarget target) {
        String approvedString = RequestCycle.get().getRequest().getParameter("ConfirmAjaxBehavior");
        boolean approved = StringUtils.isEmpty(approvedString) || Boolean.valueOf(approvedString);
        onUpdate(approved, target);
    }

    protected abstract void onUpdate(boolean approved, AjaxRequestTarget target);

    @Override
    protected CharSequence generateCallbackScript(CharSequence partialCall) {
        if (isConfirmEnabled()) {
            return "this.ConfirmAjaxBehavior=" + jsFunctionCall("confirm", messageModel.getObject().toString()) + ";" +
                    super.generateCallbackScript(partialCall + "+'&ConfirmAjaxBehavior=' + this.ConfirmAjaxBehavior");
        }

        return super.generateCallbackScript(partialCall);
    }

    public boolean isConfirmEnabled() {
        return confirmEnabled;
    }

    public void setConfirmEnabled(boolean confirmEnabled) {
        this.confirmEnabled = confirmEnabled;
    }

    @Override
    protected CharSequence getCallbackScript() {
        return super.getCallbackScript();
    }
}
