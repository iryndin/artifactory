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