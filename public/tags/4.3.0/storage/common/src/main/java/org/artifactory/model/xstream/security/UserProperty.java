package org.artifactory.model.xstream.security;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.security.UserPropertyInfo;

/**
 * @author Gidi Shabat
 */
@XStreamAlias("userProperty")
public class UserProperty implements UserPropertyInfo {

    private final transient long userId;
    private final String propKey;
    private final String propValue;

    public UserProperty(long userId, String propKey, String propValue) {
        this.userId = userId;
        this.propKey = propKey;
        this.propValue = propValue;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    @Override
    public String getPropKey() {
        return propKey;
    }

    @Override
    public String getPropValue() {
        return propValue;
    }
}
