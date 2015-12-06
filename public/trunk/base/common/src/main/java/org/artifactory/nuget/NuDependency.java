package org.artifactory.nuget;

/**
 * @author Shay Yaakov
 */
public class NuDependency {

    private String id;
    private String version;
    private String targetFramework;

    public NuDependency() {
    }

    public NuDependency(String id, String version, String targetFramework) {
        this.id = id;
        this.version = version;
        this.targetFramework = targetFramework;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTargetFramework() {
        return targetFramework;
    }

    public void setTargetFramework(String targetFramework) {
        this.targetFramework = targetFramework;
    }
}
