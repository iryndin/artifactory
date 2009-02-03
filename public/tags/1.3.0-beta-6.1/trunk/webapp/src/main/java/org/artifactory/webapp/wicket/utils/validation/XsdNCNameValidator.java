package org.artifactory.webapp.wicket.utils.validation;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;
import org.jdom.Verifier;

/**
 * Checks if a string is a valid xsd <a href="http://www.w3.org/TR/REC-xml-names/#NT-NCName"/>NCName</a>
 * string.
 *
 * @author Yossi Shaul
 */
public class XsdNCNameValidator extends StringValidator {

    /**
     * singleton instance
     */
    private static final XsdNCNameValidator INSTANCE = new XsdNCNameValidator();

    /**
     * Retrieves the singleton instance of <code>XsdNCNameValidator</code>.
     *
     * @return the singleton instance of <code>XsdNCNameValidator</code>
     */
    public static XsdNCNameValidator getInstance() {
        return INSTANCE;
    }

    @Override
    protected void onValidate(IValidatable validatable) {
        String name = (String) validatable.getValue();

        String result = Verifier.checkXMLName(name);
        if (result != null) {
            ValidationError error = new ValidationError();
            error.setMessage(result);
            validatable.error(error);
        }
    }

    /**
     * Protected constructor to force use of static singleton accessor. Override this constructor to
     * implement resourceKey(Component).
     */
    protected XsdNCNameValidator() {
    }
}