package org.artifactory.info;

/**
 * An information group for all the user info properties
 *
 * @author Noam Tenne
 */
public class UserInfo extends SystemInfoGroup {

    public UserInfo() {
        super("user.country",
                "user.dir",
                "user.home",
                "user.language",
                "user.name",
                "user.timezone");
    }
}