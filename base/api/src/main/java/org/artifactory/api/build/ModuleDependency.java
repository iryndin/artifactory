package org.artifactory.api.build;

/**
 * @author Chen Keinan
 */
public class ModuleDependency {

    private String repoKey;
    private String path;
    private String name;
    private String type;
    private String scope;
    private String sha1;
    private String status;
    private String module;

    public ModuleDependency(String repoKey, String path, String name, String type, String scope, String sha1) {
        this.repoKey = repoKey;
        if (path != null) {
            this.path = path;
        }
        this.name = name;
        this.type = type;
        this.scope = scope;
        this.sha1 = sha1;
    }

    public String getRepoKey() {
        return repoKey;
    }

    public void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }
}
