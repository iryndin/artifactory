package org.artifactory.webapp.wicket.utils;

import org.artifactory.api.mime.ContentType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.DirectoryItem;

/**
 * Enum of the files, folders and repositories CSS classes. Use cssClass() for the name of the css class.
 *
 * @author Yossi Shaul
 */
public enum CssClass {
    doc,
    folder, folderCompact("folder-compact"),
    jar, pom, xml, parent,
    repository, repositoryCache("repository-cache"),
    repositoryVirtual("repository-virtual"), root;

    private String cssClass;

    /**
     * By default the css class name is the enum name.
     */
    CssClass() {
        this.cssClass = name();
    }

    CssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    /**
     * @return String representing the css class for this enum
     */
    public String cssClass() {
        return cssClass;
    }

    /**
     * @param path The file path
     * @return The matching css class for the give file path. If there is no special css class for the given path, the
     *         generic 'doc' class will be returned.
     */
    public static CssClass getFileCssClass(String path) {
        ContentType ct = NamingUtils.getContentType(path);
        if (ct.isJarVariant()) {
            return jar;
        } else if (ct.isPom()) {
            return pom;
        } else if (ct.isXml()) {
            return xml;
        } else if (path.endsWith(DirectoryItem.UP)) {
            return parent;
        } else {
            return doc;
        }
    }
}
