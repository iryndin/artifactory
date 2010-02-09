package org.artifactory.webapp.wicket.utils.validation;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;
import org.artifactory.webapp.wicket.utils.CronUtils;

/**
 * @author Tal Abramson
 */
public class CronExpValidator extends StringValidator {

    /**
     * singleton instance
     */
    private static final CronExpValidator INSTANCE = new CronExpValidator();

    private CronExpValidator() {
        // singleton constructor
    }

    /**
     * @return the singleton instance of <code>CronExpValidator</code>
     */
    public static CronExpValidator getInstance() {
        return INSTANCE;
    }

    @Override
    protected void onValidate(IValidatable validatable) {
        String expression = (String) validatable.getValue();
        if (!CronUtils.isValid(expression)) {
            ValidationError error = new ValidationError();
            error.setMessage("Invalid cron expression");
            validatable.error(error);
        }
    }
}
