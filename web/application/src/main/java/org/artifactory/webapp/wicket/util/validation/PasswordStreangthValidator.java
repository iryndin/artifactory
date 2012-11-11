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

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;

/**
 * Validates passwords strenght (currently we only enforce minimum length)..
 *
 * @author Tal Abramson
 */
public class PasswordStreangthValidator extends StringValidator {
    private static final PasswordStreangthValidator INSTANCE = new PasswordStreangthValidator();
    private int PASSWORD_MIN_LENGTH = 4;

    private PasswordStreangthValidator() {
        // singleton private constructor
    }

    public static PasswordStreangthValidator getInstance() {
        return INSTANCE;
    }

    @Override
    protected void onValidate(IValidatable validatable) {
        String password = (String) validatable.getValue();
        ValidationError error = new ValidationError();
        if (password.length() < PASSWORD_MIN_LENGTH) {
            error.setMessage("Password too short, must be at least " +
                    PASSWORD_MIN_LENGTH + " charters long");
            validatable.error(error);
        }
    }
}