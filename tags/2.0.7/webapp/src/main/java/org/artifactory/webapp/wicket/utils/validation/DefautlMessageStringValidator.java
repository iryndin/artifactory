package org.artifactory.webapp.wicket.utils.validation;

import org.apache.wicket.validation.validator.StringValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yoavl
 */
public abstract class DefautlMessageStringValidator extends StringValidator {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(DefautlMessageStringValidator.class);


    protected final String errorMessage;

    public DefautlMessageStringValidator(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
