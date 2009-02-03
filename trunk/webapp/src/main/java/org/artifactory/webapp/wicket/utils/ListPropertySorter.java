package org.artifactory.webapp.wicket.utils;

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
     * Sorts the input string using property comparator. The property must have a getter and must
     * be primitive type or to implement Comparable.
     * The sort is not case sensitive if the property is character based.
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
