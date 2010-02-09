package org.artifactory.webapp.wicket.common.component.file.path;

import org.apache.wicket.util.file.Folder;
import static org.springframework.util.StringUtils.startsWithIgnoreCase;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author yoava
 */
public class PathHelper implements Serializable {
    private static final Pattern ROOT_PATTERN = getRootPattern();

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
            this.root = this.root + "/";
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
            if (!inputPath.startsWith("/")) {
                inputPath = "/" + inputPath;
            }

            String absPath = new File(inputPath).getAbsolutePath().replace('\\', '/');
            if (!isDriveRoot(absPath) && inputPath.endsWith("/")) {
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

    private boolean isDriveRoot(String absPath) {
        return ROOT_PATTERN.matcher(absPath).matches();
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

    private static Pattern getRootPattern() {
        StringBuilder pattern = new StringBuilder();
        pattern.append("/");
        for (File file : File.listRoots()) {
            String drive = file.getAbsolutePath().replace('\\', '/');
            pattern.append("|");
            pattern.append(drive);
            pattern.append("|");
            pattern.append(drive.toLowerCase());
        }
        return Pattern.compile(pattern.toString());
    }
}
