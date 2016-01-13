package org.artifactory.api.artifact;

/**
 * @author Gidi Shabat
 */
public class VagrantArtifactInfo implements UnitInfo {

    private String artifactType = "vagrant";
    private String path;

    public VagrantArtifactInfo() {
    }

    public VagrantArtifactInfo(String path) {
        this.path = path;
    }

    @Override
    public boolean isMavenArtifact() {
        return false;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }
}
