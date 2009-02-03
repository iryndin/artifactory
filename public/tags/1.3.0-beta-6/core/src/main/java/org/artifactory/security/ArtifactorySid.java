package org.artifactory.security;

import org.springframework.security.acls.sid.Sid;
import org.springframework.util.Assert;

/**
 * Artifactory security identity to be used for both users and groups.
 *
 * @author Yossi Shaul
 */
public class ArtifactorySid implements Sid {
    private String principal;
    private boolean group;

    /**
     * Creates new user security identity.
     *
     * @param principal The username
     */
    public ArtifactorySid(String username) {
        Assert.notNull(username, "Username required");
        principal = username;
    }

    /**
     * Created a new user or group security identity.
     *
     * @param principal The username or the group name
     * @param group     True if this identity represents a group.
     */
    public ArtifactorySid(String principal, boolean group) {
        Assert.notNull(principal, "Principal required");
        this.principal = principal;
        this.group = group;
    }

    /**
     * @return The security identity (username or groupname)
     */
    public String getPrincipal() {
        return principal;
    }

    public boolean isGroup() {
        return group;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        ArtifactorySid sid = (ArtifactorySid) other;

        if (group != sid.group) {
            return false;
        }
        if (!principal.equals(sid.principal)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = principal.hashCode();
        result = 31 * result + (group ? 1 : 0);
        return result;
    }

    public String toString() {
        return "ArtifactorySid[" + this.principal + ", isGroup: " + group + "]";
    }
}
