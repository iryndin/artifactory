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

package org.artifactory.api.build;

import org.apache.commons.lang.StringUtils;
import org.artifactory.build.BuildRun;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A custom build number comparator. If both build number are exclusively numeric, they will be compared as Longs;
 * otherwise they will be compared as strings.
 */
public class BuildNumberComparator implements Comparator<BuildRun>, Serializable {

    @Override
    public int compare(BuildRun build1, BuildRun build2) {
        if ((build1 == null) || (build2 == null)) {
            return 0;
        }

        String build1Number = build1.getNumber();
        String build2Number = build2.getNumber();

        if (StringUtils.isNumeric(build1Number) && StringUtils.isNumeric(build2Number)) {
            return Long.valueOf(build1Number).compareTo(Long.valueOf(build2Number));
        }

        return build1Number.compareTo(build2Number);
    }
}
