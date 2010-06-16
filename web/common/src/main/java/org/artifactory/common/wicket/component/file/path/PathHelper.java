/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.common.wicket.component.file.path;

import org.apache.wicket.util.file.Folder;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.springframework.util.StringUtils.startsWithIgnoreCase;

/**
 * @author yoava
 */
public class PathHelper implements Serializable {
    private static final Pattern ROOT_PATTERN = Pattern.compile(getRootPattern());
    private static final Pattern ABSOLUTE_PATTERN = Pattern.compile(getAbsolutePattern());

    private String root;

    public PathHelper() {
    }

    public PathHelper(String chRoot) {
        setRoot(chRoot);
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        if (root == null) {
            this.root = null;
            return;
        }

        this.root = new Folder(root).getAbsolutePath().replace('\\', '/');
        if (!this.root.endsWith("/")) {
            this.root += "/";
        }
    }

    public String getPathFolder(String path) {
        int slashIndex = path.lastIndexOf('/');
        if (slashIndex >= 0) {
            return path.substring(0, slashIndex + 1);
        }
        return path;
    }

    public String getAbsulotePath(String input) {
        String inputPath = input.trim().replace('\\', '/');
        if (root == null) {
            // path is absolute
            if (!isAbsolutePath(inputPath)) {
                inputPath = "/" + inputPath;
            }

            String absPath = new File(inputPath).getAbsolutePath().replace('\\', '/');
            if (!isRootPath(absPath) && inputPath.endsWith("/")) {
                absPath += '/';
            }
            return absPath;
        }

        if (inputPath.startsWith("/")) {
            inputPath = inputPath.substring(1);
        }

        inputPath = root + inputPath;
        return inputPath;
    }

    private boolean isAbsolutePath(CharSequence path) {
        return ABSOLUTE_PATTERN.matcher(path).matches();
    }

    private boolean isRootPath(CharSequence path) {
        return ROOT_PATTERN.matcher(path).matches();
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
                    String fileAbsPath = file.getAbsolutePath().replace('\\', '/');
                    if (startsWithIgnoreCase(fileAbsPath, absulotePath)) {
                        filesList.add(file);
                    }
                }
            }

            if (mask.includeFiles()) {
                for (File file : files) {
                    String fileAbsPath = file.getPath().replace('\\', '/');
                    if (startsWithIgnoreCase(fileAbsPath, absulotePath)) {
                        filesList.add(file);
                    }
                }
            }
            return filesList;
        }


        return Collections.emptyList();
    }

    public String getRelativePath(File file) {
        if (root == null) {
            return file.getAbsolutePath();
        }
        return file.getAbsolutePath().substring(root.length() - 1);
    }

    public File getAbsuloteFile(String relativePath) {
        if (relativePath == null) {
            return null;
        }

        return new File(getAbsulotePath(relativePath));
    }

    private static String getRootPattern() {
        StringBuilder pattern = new StringBuilder();
        pattern.append("/");
        for (File file : File.listRoots()) {
            String drive = file.getAbsolutePath().replace('\\', '/');
            pattern.append("|");
            pattern.append(drive);
            pattern.append("|");
            pattern.append(drive.toLowerCase());
        }
        return pattern.toString();
    }

    private static String getAbsolutePattern() {
        return "^" + getRootPattern().replace("|", "|^");
    }
}
