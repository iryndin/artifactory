package org.artifactory.webapp.wicket.security.acl;

import org.artifactory.api.security.AceInfo;

import java.io.Serializable;

/**
* Used as a model for the permissions table.
*
* @author Yossi Shaul
*/
public class AceInfoRow implements Serializable {
    private AceInfo aceInfo;

    public AceInfoRow(AceInfo aceinfo) {
        this.aceInfo = aceinfo;
    }

    public AceInfo getAceInfo() {
        return aceInfo;
    }

    public String getPrincipal() {
        return aceInfo.getPrincipal();
    }

    public void setPrincipal(String principal) {
        aceInfo.setPrincipal(principal);
    }

    public boolean isGroup() {
        return aceInfo.isGroup();
    }

    public void setGroup(boolean group) {
        aceInfo.setGroup(group);
    }

    public boolean isAdmin() {
        return aceInfo.canAdmin();
    }

    public void setAdmin(boolean admin) {
        aceInfo.setAdmin(admin);
        if (admin) {
            setDelete(true);
        }
    }

    public boolean isDelete() {
        return aceInfo.canDelete();
    }

    public void setDelete(boolean delete) {
        aceInfo.setDelete(delete);
        if (delete) {
            setDeploy(true);
        }
    }

    public boolean isDeploy() {
        return aceInfo.canDeploy();
    }

    public void setDeploy(boolean deploy) {
        aceInfo.setDeploy(deploy);
        if (deploy) {
            setRead(true);
        }
    }

    public boolean isRead() {
        return aceInfo.canRead();
    }

    public void setRead(boolean read) {
        aceInfo.setRead(read);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AceInfoRow row = (AceInfoRow) o;
        return !(aceInfo != null ? !aceInfo.equals(row.aceInfo) : row.aceInfo != null);
    }

    @Override
    public int hashCode() {
        return (aceInfo != null ? aceInfo.hashCode() : 0);
    }
}
