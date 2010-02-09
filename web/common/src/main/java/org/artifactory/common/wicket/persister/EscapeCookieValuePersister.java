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

package org.artifactory.common.wicket.persister;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.persistence.CookieValuePersister;

/**
 * Customized cookie persister that acts as a temp fix for the escaping bug (?) in Wicket. The issue: when entering a
 * backwards slash ('\') to a text component which is persistent, Wicket, on reload, will Double the amount of slashes.
 *
 * @author Noam Tenne
 */
public class EscapeCookieValuePersister extends CookieValuePersister {

    @Override
    public void load(FormComponent component) {
        Object prevInput = component.getConvertedInput();
        super.load(component);

        Object loadedInput = component.getConvertedInput();

        //If the input we have entered has changed after conversion, make sure to fix slashes
        if (prevInput != loadedInput && loadedInput != null) {
            component.setConvertedInput(loadedInput.toString().replaceAll("\\\\\\\\", "\\\\"));
            component.updateModel();
        }
    }
}
