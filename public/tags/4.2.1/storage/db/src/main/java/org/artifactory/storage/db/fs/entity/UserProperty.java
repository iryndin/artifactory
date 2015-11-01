package org.artifactory.storage.db.fs.entity;

/**
 * @author Gidi Shabat
 */
public class UserProperty {
    private final long userId;
    private final String propKey;
    private final String propValue;

    public UserProperty(long userId, String propKey, String propValue) {
        this.userId = userId;
        this.propKey = propKey;
        this.propValue = propValue;
    }

    public long getUserId() {
        return userId;
    }


    public String getPropKey() {
        return propKey;
    }

    public String getPropValue() {
        return propValue;
    }
}
