/*
 * This file is part of Artifactory.
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
 * Validates that the archive search term isn't shorter than 3 characters
 *
 * @author Noam Tenne
 */
public class ArchiveSearchValidator extends StringValidator {
    @Override
    protected void onValidate(IValidatable validatable) {
        String archiveSearchValue = (String) validatable.getValue();
        if (archiveSearchValue.length() < 4) {
            ValidationError validateError = new ValidationError();
            validateError.setMessage("Search term must be at least 4 characters long");
            validatable.error(validateError);
        }
    }
}
