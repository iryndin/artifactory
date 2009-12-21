package org.artifactory.webapp.wicket.page.build.tabs.list.compare;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.artifactory.build.api.Dependency;
import org.artifactory.common.wicket.util.ListPropertySorter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A custom sorter for the module dependencies lists
 *
 * @author Noam Y. Tenne
 */
public class DependencyItemListSorter {

    /**
     * Sorts the given list of dependencies
     *
     * @param toSort    List to sort
     * @param sortParam Selected sort param
     */
    public static void sort(List<Dependency> toSort, SortParam sortParam) {
        String sortProperty = sortParam.getProperty();
        boolean ascending = sortParam.isAscending();
        if ("scopes".equals(sortProperty)) {
            sortManual(toSort, new DependencyScopesComparator(), ascending);
        } else if ("requiredBy".equals(sortProperty)) {
            sortManual(toSort, new DependencyRequiredByComparator(), ascending);
        } else {
            ListPropertySorter.sort(toSort, sortParam);
        }
    }

    /**
     * Perform a non-property sort
     *
     * @param toSort     List to sort
     * @param comparator Comparator to sort by
     * @param ascending  True if the order should be ascending
     */
    @SuppressWarnings({"unchecked"})
    private static void sortManual(List toSort, Comparator comparator, boolean ascending) {
        if (!ascending) {
            comparator = Collections.reverseOrder(comparator);
        }
        Collections.sort(toSort, comparator);
    }
}