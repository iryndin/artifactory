package org.artifactory.webapp.wicket.common.behavior;

import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;

/**
 * A simple ajax calls decorator to display confirmation message when a button
 * is clicked.
 *
 * @author Yossi Shaul
 */
public class AjaxCallConfirmationDecorator extends AjaxPreprocessingCallDecorator {
    private String message;

    /**
     * Create the confirmation callback.
     * @param delegate  A call delegate (might be null)
     * @param message   The cpnfirmation message to display
     */
    public AjaxCallConfirmationDecorator(IAjaxCallDecorator delegate, String message) {
        super(delegate);
        this.message = message;
    }

    @Override
    public CharSequence preDecorateScript(CharSequence script) {
        return "if (!confirm('" + message + "')) return false;" +
                script;
    }
}
