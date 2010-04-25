package org.artifactory.jcr.fs;

/**
* @author freds
*/
public class FolderTreeNode {
    public final String name;
    public final FolderTreeNode[] folders;
    public final String[] poms;
    public final boolean hasMavenPlugins;

    public FolderTreeNode(String name, FolderTreeNode[] folders, String[] poms, boolean hasMavenPlugins) {
        this.name = name;
        this.folders = folders;
        this.poms = poms;
        this.hasMavenPlugins = hasMavenPlugins;
    }
}
