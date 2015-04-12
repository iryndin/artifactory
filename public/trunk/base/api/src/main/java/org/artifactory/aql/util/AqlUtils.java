package org.artifactory.aql.util;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.aql.AqlService;
import org.artifactory.aql.api.domain.sensitive.AqlApiItem;
import org.artifactory.aql.result.AqlEagerResult;
import org.artifactory.aql.result.rows.AqlBaseFullRowImpl;
import org.artifactory.aql.result.rows.AqlItem;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;

import static org.artifactory.aql.api.internal.AqlBase.and;

/**
 * General class for aql utilities
 *
 * @author Dan Feldman
 */
public class AqlUtils {

    /**
     * Returns a RepoPath from an aql result's path fields
     *
     * @param repo repo key
     * @param path path
     * @param name file name
     */
    public static RepoPath repoPathFromAql(String repo, String path, String name) {
        if (StringUtils.equals(path, ".")) {
            return RepoPathFactory.create(repo, name);
        } else {
            return RepoPathFactory.create(repo, path + "/" + name);
        }
    }

    /**
     * Returns the RepoPath that points to this aql result
     *
     * @param result AqlFullRow object that has repo, path and name fields
     */
    public static RepoPath repoPathFromAql(AqlBaseFullRowImpl result) throws IllegalArgumentException {
        if (StringUtils.isBlank(result.getRepo()) || StringUtils.isBlank(result.getPath())
                || StringUtils.isBlank(result.getName())) {
            throw new IllegalArgumentException("Repo, Path, and Name fields must contain values");
        }
        if (StringUtils.equals(result.getPath(), ".")) {
            return RepoPathFactory.create(result.getRepo(), result.getName());
        } else {
            return RepoPathFactory.create(result.getRepo(), result.getPath() + "/" + result.getName());
        }
    }

    /**
     * Returns true if the node that the path points to exists (Use with files only!)
     *
     * @param path repo path to check for existence
     */
    public static boolean exists(RepoPath path) {
        AqlSearchablePath aqlPath = new AqlSearchablePath(path);
        AqlApiItem aql = AqlApiItem.create().filter(
                and(
                        AqlApiItem.repo().equal(aqlPath.getRepo()),
                        AqlApiItem.path().equal(aqlPath.getPath()),
                        AqlApiItem.name().equal(aqlPath.getFileName())
                )
        );
        AqlEagerResult<AqlItem> results = ContextHelper.get().beanForType(AqlService.class).executeQueryEager(aql);
        return results != null && results.getResults() != null && results.getResults().size() > 0;
    }
}
