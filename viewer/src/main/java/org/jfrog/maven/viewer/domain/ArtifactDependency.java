/**
 * User: Dror Bereznitsky
 * Date: 29/11/2006
 * Time: 12:23:08
 */
package org.jfrog.maven.viewer.domain;

import org.jfrog.maven.viewer.common.ArtifactIdentifier;

import java.util.ArrayList;
import java.util.List;

public class ArtifactDependency {
    private ArtifactIdentifier dependent;
    private ArtifactIdentifier dependency;
    private String scope;
    private boolean optional;
    private List<ArtifactIdentifier> exclusions;

    public ArtifactDependency(ArtifactIdentifier dependent, ArtifactIdentifier dependency, String scope) {
        this.dependent = dependent;
        this.dependency = dependency;
        if (scope == null || scope.equals("")) {
            this.scope = org.apache.maven.artifact.Artifact.SCOPE_COMPILE;
        } else {
            this.scope = scope;
        }
        optional = false;
        exclusions = new ArrayList<ArtifactIdentifier>();
    }

    public ArtifactDependency(ArtifactIdentifier dependent, ArtifactIdentifier dependency, String scope, boolean optional) {
        this(dependent, dependency, scope);
        this.optional = optional;
    }

    public ArtifactDependency(ArtifactIdentifier dependent, ArtifactIdentifier dependency, String scope, boolean optional, List<ArtifactIdentifier> exclusions) {
        this(dependent, dependency, scope, optional);
        this.exclusions = exclusions;
    }


    public ArtifactIdentifier getDependent() {
        return dependent;
    }

    public void setDependent(ArtifactIdentifier dependent) {
        this.dependent = dependent;
    }

    public ArtifactIdentifier getDependency() {
        return dependency;
    }

    public void setDependency(ArtifactIdentifier dependency) {
        this.dependency = dependency;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }


    public List<ArtifactIdentifier> getExclusions() {
        return exclusions;
    }

    public void setExclusions(List<ArtifactIdentifier> exclusions) {
        this.exclusions = exclusions;
    }
}
