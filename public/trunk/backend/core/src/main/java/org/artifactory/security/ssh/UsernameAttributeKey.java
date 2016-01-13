package org.artifactory.security.ssh;

import org.apache.sshd.common.Session;

/**
 * @author Noam Y. Tenne
 */
public class UsernameAttributeKey extends Session.AttributeKey<String> {

    @Override
    public boolean equals(Object obj) {
        return obj instanceof UsernameAttributeKey;
    }

    /**
     * Use a constant hash code as we expect only one such attribute per sessions
     */
    @Override
    public int hashCode() {
        return 1337;
    }
}
