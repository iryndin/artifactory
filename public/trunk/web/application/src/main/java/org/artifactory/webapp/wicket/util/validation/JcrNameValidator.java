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

import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;

/**
 * @author Yoav Landman
 */
public class JcrNameValidator extends DefaultMessageStringValidator {

    public JcrNameValidator() {
        this(null);
    }

    public JcrNameValidator(String errorMessage) {
        super(errorMessage);
    }

    @Override
    protected void onValidate(IValidatable<String> validatable) {
        // Check value is a valid jcr name
        String value = validatable.getValue();
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