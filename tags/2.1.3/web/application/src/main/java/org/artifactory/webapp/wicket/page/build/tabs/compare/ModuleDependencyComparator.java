package org.artifactory.webapp.wicket.page.build.tabs.compare;

import org.artifactory.build.api.Dependency;
import org.artifactory.build.api.Module;

import java.util.Comparator;
import java.util.List;

/**
 * A custom comparator for sorting a build module's dependencies
 *
 * @author Noam Y. Tenne
 */
public class ModuleDependencyComparator implements Comparator<Module> {

    public int compare(Module module1, Module module2) {
        if ((module1 == null) || (module2 == null)) {
            return 0;
        }

        List<Dependency> module1Dependencies = module1.getDependencies();
        List<Dependency> module2Dependencies = module2.getDependencies();

        if ((module1Dependencies == null) || (module2Dependencies == null)) {
            return 0;
        }

        Integer module1Count = module1Dependencies.size();
        Integer module2Count = module2Dependencies.size();
        return module1Count.compareTo(module2Count);
    }
}