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

package org.artifactory.webapp.wicket.page.build.panel.compare;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.artifactory.api.build.BasicBuildInfo;
import org.artifactory.common.wicket.util.ListPropertySorter;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A custom sorter for the build number column. Needed since the build number might not necessarily be numeric
 *
 * @author Noam Y. Tenne
 */
public class BuildForNameListSorter {

    /**
     * Sorts the given list of builds
     *
     * @param toSort    List to sort
     * @param sortParam Selected sort param
     */
    public static void sort(List<BasicBuildInfo> toSort, SortParam sortParam) {
        String sortProperty = sortParam.getProperty();
        boolean ascending = sortParam.isAscending();
        if ("number".equals(sortProperty)) {
            sortManual(toSort, ascending);
        } else {
            ListPropertySorter.sort(toSort, sortParam);
        }
    }

    /**
     * Perform a non-property sort
     *
     * @param toSort    List to sort
     * @param ascending True if the order should be ascending
     */
    @SuppressWarnings({"unchecked"})
    private static void sortManual(List toSort, boolean ascending) {
        Comparator comparator = new BuildNumberComparator();
        if (!ascending) {
            comparator = Collections.reverseOrder(comparator);
        }
        Collections.sort(toSort, comparator);
    }

    /**
     * A custom build number comparator. If both build number are exclusively numeric, they will be compared as Longs;
     * otherwise they will be compared as strings.
     */
    private static class BuildNumberComparator implements Comparator<BasicBuildInfo>, Serializable {

        public int compare(BasicBuildInfo build1, BasicBuildInfo build2) {
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
}