package org.artifactory.webapp.wicket.utils.validation;

import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrNameValidator extends DefautlMessageStringValidator {

    public JcrNameValidator() {
        this(null);
    }

    public JcrNameValidator(String errorMessage) {
        super(errorMessage);
    }

    @Override
    protected void onValidate(IValidatable validatable) {
        // Check value is a valid jcr name
        String value = (String) validatable.getValue();
        try {
            NameParser.checkFormat(value);
        } catch (IllegalNameException e) {
            if (errorMessage == null) {
                error(validatable);
            } else {
                ValidationError error = new ValidationError();
                String message = String.format(errorMessage, value);
                error.setMessage(message);
                validatable.error(error);
            }
        }
    }
}