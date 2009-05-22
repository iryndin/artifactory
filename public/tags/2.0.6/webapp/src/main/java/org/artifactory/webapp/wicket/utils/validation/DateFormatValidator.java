package org.artifactory.webapp.wicket.utils.validation;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;

import java.text.SimpleDateFormat;

/**
 * Used to validate any date format which is entered
 *
 * @author Noam Tenne
 */
public class DateFormatValidator extends StringValidator {

    @Override
    protected void onValidate(IValidatable validatable) {
        String dateFormat = (String) validatable.getValue();
        try {
            new SimpleDateFormat(dateFormat);
        } catch (IllegalArgumentException e) {
            ValidationError validateError = new ValidationError();
            validateError.setMessage("Please enter a valid date format.");
            validatable.error(validateError);
        }
    }
}
