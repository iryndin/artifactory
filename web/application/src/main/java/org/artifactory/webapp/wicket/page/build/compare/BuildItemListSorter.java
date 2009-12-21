package org.artifactory.webapp.wicket.page.build.compare;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.artifactory.build.api.Build;
import org.artifactory.common.wicket.util.ListPropertySorter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A custom sorter for the build published module lists
 *
 * @author Noam Y. Tenne
 */
public class BuildItemListSorter {

    /**
     * Sorts the given list of builds
     *
     * @param toSort    List to sort
     * @param sortParam Selected sort param
     */
    public static void sort(List<Build> toSort, SortParam sortParam) {
        String sortProperty = sortParam.getProperty();
        boolean ascending = sortParam.isAscending();
        if ("startedDate".equals(sortProperty)) {
            sortManual(toSort, new BuildStartedDateComparator(), ascending);
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
