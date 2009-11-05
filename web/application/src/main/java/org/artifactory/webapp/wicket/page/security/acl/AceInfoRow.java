/*
 * This file is part of Artifactory.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.webapp.wicket.page.security.acl;

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
            setAnnotate(true);
        }
    }

    public boolean isAnnotate() {
        return aceInfo.canAnnotate();
    }

    public void setAnnotate(boolean annotate) {
        aceInfo.setAnnotate(annotate);
        if (annotate) {
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
