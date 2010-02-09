package org.artifactory.webapp.wicket.page.build.tabs.list.compare;

import org.apache.commons.lang.StringUtils;
import org.artifactory.build.api.Dependency;

import java.util.Comparator;
import java.util.List;

/**
 * A custom comparator for sorting a module dependency's required-by list
 *
 * @author Noam Y. Tenne
 */
public class DependencyRequiredByComparator implements Comparator<Dependency> {

    public int compare(Dependency dependency1, Dependency dependency2) {
        if ((dependency1 == null) || (dependency2 == null)) {
            return 0;
        }

        List<String> dependency1RequiredBy = dependency1.getRequiredBy();
        List<String> dependency2RequiredBy = dependency2.getRequiredBy();

        if ((dependency1RequiredBy == null) || (dependency2RequiredBy == null)) {
            return 0;
        }

        String dependency1Display = getRequiredBy(dependency1RequiredBy);
        String dependency2Display = getRequiredBy(dependency2RequiredBy);
        return dependency1Display.compareTo(dependency2Display);
    }

    /**
     * Returns the display value of the required-by
     *
     * @return Display dependency required-by
     */
    public String getRequiredBy(List<String> dependencyRequiredBy) {
        StringBuilder builder = new StringBuilder();
        for (String requiredBy : dependencyRequiredBy) {
            if (StringUtils.isNotBlank(requiredBy)) {
                int requiredByIndex = dependencyRequiredBy.indexOf(requiredBy);
                builder.append(requiredBy);
                if (requiredByIndex < (dependencyRequiredBy.size() - 1)) {
                    builder.append(";");
                }
            }
        }

        return builder.toString();
    }
}