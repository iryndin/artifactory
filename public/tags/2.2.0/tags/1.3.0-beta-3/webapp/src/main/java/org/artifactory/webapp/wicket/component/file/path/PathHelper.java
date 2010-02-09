package org.artifactory.webapp.wicket.component.file.path;

import org.apache.wicket.util.file.Folder;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author yoava
 */
public class PathHelper implements Serializable {
    private String root;
    private String rootDir;

    public PathHelper(String root) {
        setRoot(root);
        rootDir = root;
    }

    public String getRootDir() {
        return rootDir;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = new Folder(root).getAbsolutePath().replace('\\', '/');
        if (!this.root.endsWith("/")) {
            this.root = this.root + "/";
        }
    }

    public String getPathFolder(final String path) {
        int slash_index = path.lastIndexOf('/');
        if (slash_index >= 0) {
            return path.substring(0, slash_index + 1);
        }
        return path;
    }

    public String getAbsulotePath(String input) {
        String inputPath = input.trim().replace('\\', '/');
        if (inputPath.startsWith("/")) {
            inputPath = inputPath.substring(1);
        }
        inputPath = root + inputPath;
        return inputPath;
    }

    public List<File> getFiles(String path, PathMask mask) {
        String absulotePath = getAbsulotePath(path);
        Folder folder = new Folder(getPathFolder(absulotePath));
        if (folder.exists() && folder.isDirectory()) {
            Folder[] folders = folder.getFolders();
            File[] files = folder.getFiles();
            List<File> filesList = new ArrayList<File>(folders.length + files.length);

            if (mask.includeFolders()) {
                for (Folder file : folders) {
                    String fileAbsPath = file.getPath().replace('\\', '/');
                    if (fileAbsPath.startsWith(absulotePath)) {
                        filesList.add(file);
                    }
                }
            }

            if (mask.includeFiles()) {
                for (File file : files) {
                    String fileAbsPath = file.getPath().replace('\\', '/');
                    if (fileAbsPath.startsWith(absulotePath)) {
                        filesList.add(file);
                    }
                }
            }
            return filesList;
        }


        return Collections.emptyList();
    }

    public String getRelativePath(File file) {
        return file.getAbsolutePath().substring(root.length() - 1);
    }

    public File getFile(String relativePath) {
        if (relativePath == null) {
            return null;
        }

        String relPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return new File(root + relPath.replace('\\', '/'));
    }
}
