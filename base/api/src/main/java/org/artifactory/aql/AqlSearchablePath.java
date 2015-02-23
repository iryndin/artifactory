package org.artifactory.aql;

import com.google.common.collect.Lists;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.util.PathUtils;

import java.util.List;

/**
 * This class represents an AQL searchable path.
 * Path elements are separated to repo key, path, and file name to support easier searches when using the AQL's
 * repo(), path() and name() fields.
 * ALL 3 FIELDS MUST BE SUPPLIED TO THE CONSTRUCTORS
 * <p/>
 * This class can accept either RepoPath or a full path as a string for conversion.
 * Available wildcards for search operations are ?(character) and *(word).
 * <p/>
 * Syntax examples for using this util correctly:
 * [**] - search recursively in all sub folders (used in path section only)
 * [*.ext] - all files with extension .ext      (used in fileName section only)
 * [*.*] - all files                            (used in fileName section only)
 * [*.?ar] - all files ending with any character followed by 'ar' (war, jar, ear, etc.)
 * <p/>
 * repo2/org/jfrog/test/*&#42;/*.*          -  all files under all of org/jfrog/test sub folders
 * repo2/org/jfrog/test/module&#42;/*.jar   -  all jar files under  org/jfrog/test sub folders that start with module
 *
 * @author danf
 */
public class AqlSearchablePath {

    private String repo;
    private String path;
    private String fileName;

    public AqlSearchablePath(String repo, String path, String fileName) {
        this.repo = repo;
        this.path = path;
        this.fileName = fileName;
    }

    public AqlSearchablePath(String fullPath) {
        repo = PathUtils.getFirstPathElement(fullPath);
        path = PathUtils.getParent(PathUtils.stripFirstPathElement(fullPath));
        fileName = PathUtils.getFileName(fullPath);
    }

    public AqlSearchablePath(RepoPath repoPath) {
        repo = repoPath.getRepoKey();
        path = repoPath.getParent().getPath(); //OK to have NPE here, the path should always point to a file (or *.*)
        fileName = repoPath.getName();
    }

    public static List<AqlSearchablePath> fullPathToSearchablePathList(List<String> fullPaths) {
        List<AqlSearchablePath> outList = Lists.newArrayList();
        for (String fullPath : fullPaths) {
            outList.add(new AqlSearchablePath(fullPath));
        }
        return outList;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String toFullPath() {
        return repo + "/" + path + "/" + fileName;
    }

    public RepoPath toRepoPath() {
        return InternalRepoPathFactory.create(repo, path + "/" + fileName);
    }
}
