package org.artifactory.webapp.wicket.utils.validation;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;

/**
 * Checks if an xml id is unique in the central config descriptor.
 *
 * @author Yossi Shaul
 */
public class UniqueXmlIdValidator extends StringValidator {
    private MutableCentralConfigDescriptor centralConfig;

    public UniqueXmlIdValidator(MutableCentralConfigDescriptor centralConfig) {
        this.centralConfig = centralConfig;
    }

    @Override
    protected void onValidate(IValidatable validatable) {
        String key = (String) validatable.getValue();
        if (!centralConfig.isKeyAvailable(key)) {
            ValidationError error = new ValidationError();
            error.setMessage(String.format("The key '%s' is already used", key));
            validatable.error(error);
        }
    }
}