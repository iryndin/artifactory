package org.artifactory.webapp.wicket.utils.validation;

import org.apache.wicket.validation.validator.StringValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;

/**
 * Validates passwords strenght (currently we only enforce minimum length)..
 *
 * @author Tal Abramson
 */
public class PasswordStreangthValidator extends StringValidator {
    private static final PasswordStreangthValidator INSTANCE = new PasswordStreangthValidator();
    private int PASSWORD_MIN_LENGTH = 4;

    private PasswordStreangthValidator() {
        // singleton private constructor
    }

    public static PasswordStreangthValidator getInstance() {
        return INSTANCE;
    }

    @Override
    protected void onValidate(IValidatable validatable) {
        String password = (String) validatable.getValue();
        ValidationError error = new ValidationError();
        if (password.length() < PASSWORD_MIN_LENGTH) {
            error.setMessage("Password too short, must be at least " +
                    PASSWORD_MIN_LENGTH + " charters long");
            validatable.error(error);
        }
    }
}