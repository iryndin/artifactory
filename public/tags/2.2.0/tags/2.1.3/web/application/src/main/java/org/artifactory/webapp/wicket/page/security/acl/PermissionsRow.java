package org.artifactory.webapp.wicket.page.security.acl;

import org.artifactory.api.security.PermissionTargetInfo;

import java.io.Serializable;

/**
 * @author Tomer Cohen
 */
public class PermissionsRow implements Serializable {
    private PermissionTargetInfo permissionTarget;
    private boolean read;
    private boolean annotate;
    private boolean deploy;
    private boolean delete;
    private boolean admin;

    public PermissionsRow(PermissionTargetInfo permissionTarget) {
        this.permissionTarget = permissionTarget;
    }

    public PermissionTargetInfo getPermissionTarget() {
        return permissionTarget;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isAnnotate() {
        return annotate;
    }

    public void setAnnotate(boolean annotate) {
        this.annotate = annotate;
    }

    public boolean isDeploy() {
        return deploy;
    }

    public void setDeploy(boolean deploy) {
        this.deploy = deploy;
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean hasPermissions() {
        return isRead() || isDeploy() || isDelete() || isAnnotate() || isAdmin();
    }
}
