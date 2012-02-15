/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.webapp.wicket.util.validation;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;
import org.artifactory.util.PathUtils;

import java.util.Arrays;

/**
 * Validates a URI. We use our own validator since the UrlValidator of wicket is broken in 1.3.5
 * (http://issues.apache.org/jira/browse/WICKET-1926).
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
            URI parsedUri = new URI(uri, false);
            String scheme = parsedUri.getScheme();
            if (!anySchemaAllowed() && StringUtils.isBlank(scheme)) {
                addError(validatable, String.format(
                        "Url scheme cannot be empty. The following schemes are allowed: %s. " +
                                "For example: %s://host",
                        Arrays.asList(allowedSchemes), allowedSchemes[0]));

            } else if (!allowedSchema(scheme)) {
                addError(validatable, String.format(
                        "Scheme '%s' is not allowed. The following schemes are allowed: %s",
                        scheme, Arrays.asList(allowedSchemes)));
            }

            String host = parsedUri.getHost();
            if (host == null) {
                addError(validatable, "Cannot resolve host from url");
            }
        } catch (URIException e) {
            addError(validatable, String.format("'%s' is not a valid url", uri));
        }
    }

    private boolean allowedSchema(String scheme) {
        if (anySchemaAllowed()) {
            return true;
        }

        for (String allowedScheme : allowedSchemes) {
            if (allowedScheme.equalsIgnoreCase(scheme)) {
                return true;
            }
        }
        return false;
    }

    private boolean anySchemaAllowed() {
        return allowedSchemes == null || allowedSchemes.length == 0;
    }

    private void addError(IValidatable validatable, String message) {
        ValidationError error = new ValidationError();
        error.setMessage(message);
        validatable.error(error);
    }
}