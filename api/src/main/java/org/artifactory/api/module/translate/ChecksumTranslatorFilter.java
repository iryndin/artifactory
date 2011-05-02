/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.api.module.translate;

import org.artifactory.api.mime.NamingUtils;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.util.PathUtils;

/**
 * Filters checksum extensions
 *
 * @author Noam Y. Tenne
 */
public class ChecksumTranslatorFilter implements TranslatorFilter {

    public boolean filterRequired(String path) {
        return NamingUtils.isChecksum(path);
    }

    public String getFilteredContent(String path) {
        ChecksumType checksumType = ChecksumType.forFilePath(path);
        if (checksumType == null) {
            return null;
        }
        return checksumType.ext();
    }

    public String stripPath(String path) {
        return PathUtils.stripExtension(path);
    }

    public String applyFilteredContent(String strippedPath, String filteredContent) {
        return strippedPath + filteredContent;
    }
}
