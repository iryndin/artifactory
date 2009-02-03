package org.artifactory.webapp.wicket.security.acl;

import org.artifactory.api.security.PermissionTargetInfo;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public enum CommonPathPattern {

    NONE("None", ""),
    ANY("Any", PermissionTargetInfo.ANY_PATH),
    SOURCES("Source artifacts", "**/*-sources.*"),
    SNAPSHOTS("Snapshot artifacts", "**/*-SNAPSHOT/**"),
    PACKAGES("Artifacts of package com.acme", "com/acme/**");

    private String displayName;
    private String pattern;

    CommonPathPattern(String displayName, String pattern) {
        this.displayName = displayName;
        this.pattern = pattern;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
