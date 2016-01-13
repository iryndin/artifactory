package org.artifactory.ui.rest.model.admin.security.user;

import org.artifactory.rest.common.model.RestModel;
import org.artifactory.rest.common.util.JsonUtil;
import org.artifactory.security.UserConfigurationImpl;

import java.util.List;

/**
 * @author Chen Keinan
 */
public class BaseUser extends UserConfigurationImpl implements RestModel {

    private boolean proWithoutLicense;

    public BaseUser(){}

    private Boolean canDeploy;
    private Boolean canManage;
    private Boolean preventAnonAccessBuild;
    private List<UserPermissions> permissionsList;
    private String externalRealmLink;
    private Boolean anonAccessEnabled;
    private boolean existsInDB;
    private boolean requireProfileUnlock;
    private boolean requireProfilePassword;
    private Boolean locked;
    private Boolean credentialsExpired;
    private Integer currentPasswordValidFor;

    public void setProWithoutLicense(boolean proWithoutLicense) {
        this.proWithoutLicense = proWithoutLicense;
    }

    public boolean isProWithoutLicense() {
        return proWithoutLicense;
    }

    public BaseUser (String userName,boolean admin){
        super.setAdmin(admin);
        super.setName(userName);
    }

    public Boolean isCanDeploy() {
        return canDeploy;
    }

    public void setCanDeploy(Boolean canDeploy) {
        this.canDeploy = canDeploy;
    }

    public Boolean isCanManage() {
        return canManage;
    }

    public void setCanManage(Boolean canManage) {
        this.canManage = canManage;
    }

    public Boolean isPreventAnonAccessBuild() {
        return preventAnonAccessBuild;
    }

    public void setPreventAnonAccessBuild(Boolean preventAnonAccessBuild) {
        this.preventAnonAccessBuild = preventAnonAccessBuild;
    }

    public List<UserPermissions> getPermissionsList() {
        return permissionsList;
    }

    public void setPermissionsList(List<UserPermissions> permissionsList) {
        this.permissionsList = permissionsList;
    }


    public String getExternalRealmLink() {
        return externalRealmLink;
    }

    public void setExternalRealmLink(String externalRealmLink) {
        this.externalRealmLink = externalRealmLink;
    }

    public Boolean getAnonAccessEnabled() {
        return anonAccessEnabled;
    }

    public void setAnonAccessEnabled(Boolean anonAccessEnabled) {
        this.anonAccessEnabled = anonAccessEnabled;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public boolean isExistsInDB() { return existsInDB; }

    public void setExistsInDB(boolean existsInDB) { this.existsInDB = existsInDB; }

    public boolean isRequireProfileUnlock() {
        return requireProfileUnlock;
    }

    public boolean isRequireProfilePassword() {
        return requireProfilePassword;
    }

    public void setRequireProfilePassword(boolean requireProfilePassword) {
        this.requireProfilePassword = requireProfilePassword;
    }

    public void setRequireProfileUnlock(boolean requireProfileUnlock) {
        this.requireProfileUnlock = requireProfileUnlock;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }

    public Boolean getCredentialsExpired() {
        return credentialsExpired;
    }

    public void setCredentialsExpired(Boolean credentialsExpired) {
        this.credentialsExpired = credentialsExpired;
    }

    /**
     * @return number of days till password should be changed
     */
    public Integer getCurrentPasswordValidFor() {
        return currentPasswordValidFor;
    }

    /**
     * @param currentPasswordValidFor number of days till password should be changed
     */
    public void setCurrentPasswordValidFor(Integer currentPasswordValidFor) {
        this.currentPasswordValidFor = currentPasswordValidFor;
    }
}
