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

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;
import org.artifactory.addon.AddonsManager;
import org.artifactory.api.context.ContextHelper;

/**
 * The Server ID field Wicket validator
 *
 * @author Noam Y. Tenne
 */
public class ServerIdValidator extends StringValidator {

    @Override
    protected void onValidate(IValidatable validatable) {
        String expression = (String) validatable.getValue();
        expression = expression.trim();
        try {
            AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
            if (!addonsManager.isServerIdValid(expression)) {
                postError(validatable, "Invalid Server ID.");
            }
        } catch (Exception e) {
            postError(validatable, e.getMessage());
        }
    }

    /**
     * Posts a validation error with the given message
     *
     * @param validatable  Validatable object
     * @param errorMessage Message to display
     */
    private void postError(IValidatable validatable, String errorMessage) {
        ValidationError error = new ValidationError();
        error.setMessage(errorMessage);
        validatable.error(error);
    }
}
