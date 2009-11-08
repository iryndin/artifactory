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

package org.artifactory.common.wicket.util;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.springframework.beans.support.MutableSortDefinition;
import org.springframework.beans.support.PropertyComparator;
import org.springframework.beans.support.SortDefinition;

import java.util.List;

/**
 * Sorts the input list by a property of its members in ascending or descending order.
 *
 * @author Yossi Shaul
 */
public class ListPropertySorter {

    /**
     * Sorts the input string using property comparator. The property must have a getter and must be primitive type or
     * to implement Comparable. The sort is not case sensitive if the property is character based.
     *
     * @param listToSort Modifiable list to sort
     * @param sortParam  The property to sort by and is ascending or descending.
     */
    public static void sort(List listToSort, SortParam sortParam) {
        if (sortParam != null) {
            String property = sortParam.getProperty();
            SortDefinition sortDefinition = new MutableSortDefinition(property, true, sortParam.isAscending());
            PropertyComparator.sort(listToSort, sortDefinition);
        }
    }
}
