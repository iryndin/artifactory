package org.artifactory.webapp.wicket.util.validation;

import org.apache.wicket.validation.validator.NumberValidator;

/**
 * The port number field wicket validator. Asserts the input is no lower than 1, and no higher than 65535 (the valid
 * port range)
 *
 * @author Noam Y. Tenne
 */
public class PortNumberValidator extends NumberValidator.RangeValidator {

    /**
     * Default constructor
     */
    public PortNumberValidator() {
        super(1L, 65535L);
    }
}
