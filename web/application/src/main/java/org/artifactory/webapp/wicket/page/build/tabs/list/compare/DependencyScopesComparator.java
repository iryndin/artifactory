package org.artifactory.webapp.wicket.page.build.tabs.list.compare;

import org.apache.commons.lang.StringUtils;
import org.artifactory.build.api.Dependency;

import java.util.Comparator;
import java.util.List;

/**
 * A custom comparator for sorting a module dependency's scopes list
 *
 * @author Noam Y. Tenne
 */
public class DependencyScopesComparator implements Comparator<Dependency> {

    public int compare(Dependency dependency1, Dependency dependency2) {
        if ((dependency1 == null) || (dependency2 == null)) {
            return 0;
        }

        List<String> dependency1Scopes = dependency1.getScopes();
        List<String> dependency2Scopes = dependency2.getScopes();

        if ((dependency1Scopes == null) || (dependency2Scopes == null)) {
            return 0;
        }

        String dependency1Display = getScopes(dependency1Scopes);
        String dependency2Display = getScopes(dependency2Scopes);
        return dependency1Display.compareTo(dependency2Display);
    }

    /**
     * Returns the display value of the scopes
     *
     * @return Display dependency scopes
     */
    private String getScopes(List<String> dependencyScopes) {
        StringBuilder builder = new StringBuilder();
        for (String scope : dependencyScopes) {
            if (StringUtils.isNotBlank(scope)) {
                int scopeIndex = dependencyScopes.indexOf(scope);
                builder.append(scope);
                if (scopeIndex < (dependencyScopes.size() - 1)) {
                    builder.append(";");
                }
            }
        }

        return builder.toString();
    }
}