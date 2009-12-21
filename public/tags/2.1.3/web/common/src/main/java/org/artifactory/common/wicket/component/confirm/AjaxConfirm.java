package org.artifactory.common.wicket.component.confirm;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;

import static org.artifactory.common.wicket.util.JavaScriptUtils.jsFunctionCall;

/**
 * @author Yoav Aharoni
 */
public class AjaxConfirm {
    private static final AjaxConfirm INSTANCE = new AjaxConfirm();

    private AjaxConfirm() {
    }

    public static AjaxConfirm get() {
        return INSTANCE;
    }

    public void confirm(ConfirmDialog dialog) {
        final ConfirmAjaxBehavior eventBehavior = new ConfirmAjaxBehavior(dialog);
        RequestCycle.get().getResponsePage().add(eventBehavior);
        final AjaxRequestTarget target = AjaxRequestTarget.get();
        target.appendJavascript(eventBehavior.getConfirmScript());
    }

    private static class ConfirmAjaxBehavior extends AjaxEventBehavior {
        private final ConfirmDialog dialog;

        private ConfirmAjaxBehavior(ConfirmDialog dialog) {
            super("wicket:confirm");
            this.dialog = dialog;
        }

        @Override
        protected void onEvent(AjaxRequestTarget target) {
            String approvedString = RequestCycle.get().getRequest().getParameter("confirm");
            boolean approved = StringUtils.isEmpty(approvedString) || Boolean.valueOf(approvedString);
            dialog.onConfirm(approved, target);
            getComponent().remove(this);
        }

        public String getConfirmScript() {
            return getCallbackScript().toString();
        }

        @Override
        protected CharSequence generateCallbackScript(CharSequence partialCall) {
            return "var ok=" + jsFunctionCall("confirm", dialog.getMessage()) + ";" +
                    super.generateCallbackScript(partialCall + "+'&confirm=' + ok");
        }
    }
}
