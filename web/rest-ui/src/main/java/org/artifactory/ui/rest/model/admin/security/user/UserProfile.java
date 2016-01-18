package org.artifactory.ui.rest.model.admin.security.user;

import org.artifactory.rest.common.model.BaseModel;
import org.artifactory.ui.rest.model.admin.configuration.bintray.BintrayUIModel;
import org.artifactory.ui.rest.model.admin.configuration.ssh.SshClientUIModel;

/**
 * @author Chen Keinan
 */
public class UserProfile extends BaseModel {

    private User user;
    private BintrayUIModel bintray;
    private SshClientUIModel ssh;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BintrayUIModel getBintray() {
        return bintray;
    }

    public void setBintray(BintrayUIModel bintray) {
        this.bintray = bintray;
    }

    public SshClientUIModel getSsh() {
        return ssh;
    }

    public void setSsh(SshClientUIModel ssh) {
        this.ssh = ssh;
    }
}
