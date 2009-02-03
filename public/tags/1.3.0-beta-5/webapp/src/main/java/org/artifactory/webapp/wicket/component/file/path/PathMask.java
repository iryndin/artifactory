package org.artifactory.webapp.wicket.component.file.path;

/**
 * @author yoava
 */
public enum PathMask {
    FILES,
    FOLDERS,
    ALL;

    public boolean includeFolders() {
        return !equals(FILES);
    }

    public boolean includeFiles() {
        return !equals(FOLDERS);
    }
}
