package org.artifactory.webapp.wicket.utils.validation;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;
import org.artifactory.util.PathUtils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Validates a URI. We use our own validator since the UrlValidator of wicket is broken in 1.3.5.
 *
 * @author Yossi Shaul
 */
public class UriValidator extends StringValidator {

    private String[] allowedSchemes;

    /**
     * Creates new URI validator.
     *
     * @param allowedSchemes List of allowed uri schemes (http, ldap, etc.). If empty all schemes are allowed.
     */
    public UriValidator(String... allowedSchemes) {
        this.allowedSchemes = allowedSchemes;
    }

    @Override
    protected void onValidate(IValidatable validatable) {
        String uri = (String) validatable.getValue();

        if (!PathUtils.hasText(uri)) {
            addError(validatable, "The URL cannot be empty");
            return;
        }

        try {
            URI parsedUri = new URI(uri);
            String scheme = parsedUri.getScheme();
            if (!allowedSchema(scheme)) {
                addError(validatable, String.format("Scheme '%s' is not allowed", scheme));
            }
        } catch (URISyntaxException e) {
            addError(validatable, String.format("'%s' is not a valid url", uri));
        }
    }

    private boolean allowedSchema(String scheme) {
        if (allowedSchemes == null || allowedSchemes.length == 0) {
            return true;
        }

        for (String allowedScheme : allowedSchemes) {
            if (allowedScheme.equalsIgnoreCase(scheme)) {
                return true;
            }
        }
        return false;
    }

    private void addError(IValidatable validatable, String message) {
        ValidationError error = new ValidationError();
        error.setMessage(message);
        validatable.error(error);
    }
}