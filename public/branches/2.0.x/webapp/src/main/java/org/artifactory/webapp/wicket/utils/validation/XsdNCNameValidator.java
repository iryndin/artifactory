package org.artifactory.webapp.wicket.utils.validation;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.jdom.Verifier;

/**
 * Checks if a string is a valid xsd <a href="http://www.w3.org/TR/REC-xml-names/#NT-NCName"/>NCName</a> string.
 *
 * @author Yossi Shaul
 */
public class XsdNCNameValidator extends DefautlMessageStringValidator {

    public XsdNCNameValidator() {
        this(null);
    }

    public XsdNCNameValidator(String errorMessage) {
        super(errorMessage);
    }

    @Override
    protected void onValidate(IValidatable validatable) {
        String value = (String) validatable.getValue();

        String result = Verifier.checkXMLName(value);
        if (result != null) {
            ValidationError error = new ValidationError();
            String message = errorMessage == null ? result : String.format(errorMessage, value);
            error.setMessage(message);
            validatable.error(error);
        }
    }
}