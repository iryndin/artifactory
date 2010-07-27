package org.artifactory.jcr.fs;

import java.util.Calendar;

/**
 * @author freds
 */
public class FolderTreeNode {
    public final String name;
    public final Calendar created;
    public final FolderTreeNode[] folders;
    public final String[] poms;
    public final boolean hasMavenPlugins;

    public FolderTreeNode(String name, Calendar created, FolderTreeNode[] folders, String[] poms,
            boolean hasMavenPlugins) {
        this.name = name;
        this.created = created;
        this.folders = folders;
        this.poms = poms;
        this.hasMavenPlugins = hasMavenPlugins;
    }
}
