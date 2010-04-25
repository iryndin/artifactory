/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.PathParser;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;

/**
 * Validates that the deploy target path is a valid JCR one. And that checksums cannot be uploaded through the UI.
 *
 * @author Tomer Cohen
 */
public class DeployTargetPathValidator extends StringValidator {

    @Override
    protected void onValidate(IValidatable validatable) {
        String targetPath = (String) validatable.getValue();
        // Check for valid JCR path
        try {
            PathParser.checkFormat(targetPath);
        } catch (MalformedPathException e) {
            ValidationError validateError = new ValidationError();
            validateError.setMessage(e.getMessage());
            validatable.error(validateError);
        }
    }
}
