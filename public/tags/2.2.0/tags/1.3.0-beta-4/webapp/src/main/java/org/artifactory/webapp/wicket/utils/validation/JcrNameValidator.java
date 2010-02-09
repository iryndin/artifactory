package org.artifactory.webapp.wicket.utils.validation;

import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.StringValidator;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrNameValidator extends StringValidator {

    /**
     * singleton instance
     */
    private static final JcrNameValidator INSTANCE = new JcrNameValidator();

    /**
     * Retrieves the singleton instance of <code>JcrNameValidator</code>.
     *
     * @return the singleton instance of <code>JcrNameValidator</code>
     */
    public static JcrNameValidator getInstance() {
        return INSTANCE;
    }

    protected void onValidate(IValidatable validatable) {
        // Check value is a valid jcr name
        String name = (String) validatable.getValue();
        try {
            NameParser.checkFormat(name);
        } catch (IllegalNameException e) {
            error(validatable);
        }
    }

    /**
     * Protected constructor to force use of static singleton accessor. Override this constructor to
     * implement resourceKey(Component).
     */
    protected JcrNameValidator() {
        super();
    }
}