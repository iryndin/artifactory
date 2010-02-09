/**
 * User: Dror Bereznitsky
 * Date: 31/12/2006
 * Time: 00:38:27
 */
package org.jfrog.maven.viewer.domain;

import org.apache.maven.project.MavenProject;
import org.jfrog.maven.viewer.common.ArtifactIdentifier;

import java.util.ArrayList;
import java.util.List;

public class MockArtifact implements Artifact {
    private ArtifactIdentifier identifier;
    private String scope;
    private ArtifactIdentifier dependent;

    public MockArtifact(ArtifactIdentifier identifier) {
        this.identifier = identifier;
        scope = "compile";
    }

    public void accept(ArtifactVisitor visitor) {
        visitor.visitArtifact(this);
    }

    public ArtifactIdentifier getDependent() {
        return dependent;
    }

    public boolean hasDependent() {
        return (dependent != null);
    }

    public void setDependent(ArtifactIdentifier dependent) {
        this.dependent = dependent;
    }

    public ArtifactIdentifier getIdentifier() {
        return identifier;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public MavenProject getMavenProject() {
        return null;
    }


    public List<ArtifactDependency> getDependencies() {
        return new ArrayList<ArtifactDependency>();
    }

    public void setDependencies(List<ArtifactDependency> dependencies) {
    }
}
