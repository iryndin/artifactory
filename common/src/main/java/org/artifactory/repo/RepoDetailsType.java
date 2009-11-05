package org.artifactory.repo;

/**
 * An object that defines the type for the repository provisioning
 *
 * @author Noam Y. Tenne
 */
public enum RepoDetailsType {
    LOCAL("Local"), REMOTE("Remote"), VIRTUAL("Virtual");

    private String typeName;

    /**
     * Main constructor
     *
     * @param typeName The display name of the type
     */
    RepoDetailsType(String typeName) {
        this.typeName = typeName;
    }

    /**
     * Returns the display name of the type
     *
     * @return Type display name
     */
    public String getTypeName() {
        return typeName;
    }
}
