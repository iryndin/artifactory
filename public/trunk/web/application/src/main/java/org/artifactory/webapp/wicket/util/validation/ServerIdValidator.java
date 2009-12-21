package org.artifactory.webapp.wicket.util.validation;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;
import org.artifactory.addon.AddonsManager;
import org.artifactory.api.context.ContextHelper;

/**
 * The Server ID field Wicket validator
 *
 * @author Noam Y. Tenne
 */
public class ServerIdValidator extends StringValidator {

    @Override
    protected void onValidate(IValidatable validatable) {
        String expression = (String) validatable.getValue();
        expression = expression.trim();
        try {
            AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
            if (!addonsManager.isServerIdValid(expression)) {
                postError(validatable, "Invalid Server ID.");
            }
        } catch (Exception e) {
            postError(validatable, e.getMessage());
        }
    }

    /**
     * Posts a validation error with the given message
     *
     * @param validatable  Validatable object
     * @param errorMessage Message to display
     */
    private void postError(IValidatable validatable, String errorMessage) {
        ValidationError error = new ValidationError();
        error.setMessage(errorMessage);
        validatable.error(error);
    }
}
