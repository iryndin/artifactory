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

package org.artifactory.common.wicket.util;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.Form;

/**
 * @author Yoav Landman
 */
public class ComponentUtils {

    public static void updatePersistentFormComponents(MarkupContainer container) {
        container.visitChildren(Form.class, new Component.IVisitor<Form>() {
            //For each FormComponent found on the Page (not Form)
            public Object component(Form form) {
                form.loadPersistentFormComponentValues();
                return CONTINUE_TRAVERSAL;
            }
        });
    }
}