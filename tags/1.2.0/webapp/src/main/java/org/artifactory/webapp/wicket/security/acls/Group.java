package org.artifactory.webapp.wicket.security.acls;

import org.apache.log4j.Logger;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class Group implements Serializable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(Group.class);

    private String groupId;

    public Group() {
    }

    public Group(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
