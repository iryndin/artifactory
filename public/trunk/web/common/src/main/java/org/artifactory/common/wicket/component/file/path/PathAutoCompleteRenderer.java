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

package org.artifactory.common.wicket.component.file.path;

import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteTextRenderer;

import java.io.File;

/**
 * @author yoava
 */
public class PathAutoCompleteRenderer extends AbstractAutoCompleteTextRenderer<File> {
    private PathHelper pathHelper;

    public PathAutoCompleteRenderer(PathHelper pathHelper) {
        this.pathHelper = pathHelper;
    }

    @Override
    protected String getTextValue(File file) {
        if (file == null) {
            return "";
        }

        return pathHelper.getRelativePath(file);
    }
}
