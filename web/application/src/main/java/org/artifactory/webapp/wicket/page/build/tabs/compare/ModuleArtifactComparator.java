package org.artifactory.webapp.wicket.page.build.tabs.compare;

import org.artifactory.build.api.Artifact;
import org.artifactory.build.api.Module;

import java.util.Comparator;
import java.util.List;

/**
 * A custom comparator for sorting a build module's published artifacts
 *
 * @author Noam Y. Tenne
 */
public class ModuleArtifactComparator implements Comparator<Module> {

    public int compare(Module module1, Module module2) {
        if ((module1 == null) || (module2 == null)) {
            return 0;
        }

        List<Artifact> module1Artifacts = module1.getArtifacts();
        List<Artifact> module2Artifacts = module2.getArtifacts();

        if ((module1Artifacts == null) || (module2Artifacts == null)) {
            return 0;
        }

        Integer module1Count = module1Artifacts.size();
        Integer module2Count = module2Artifacts.size();
        return module1Count.compareTo(module2Count);
    }
}