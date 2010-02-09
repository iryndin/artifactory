package org.artifactory.build;

import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.md.Properties;
import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.build.api.Build;
import org.artifactory.build.api.BuildService;
import org.artifactory.spring.ReloadableBean;

import java.util.List;
import java.util.Map;

/**
 * The system-internal interface of the build service
 *
 * @author Noam Y. Tenne
 */
public interface InternalBuildService extends ReloadableBean, BuildService {

    /**
     * Adds the given build to the DB within a transaction
     *
     * @param build Build to add
     */
    @Lock(transactional = true)
    void transactionalAddBuild(Build build);

    /**
     * Returns the best matching file info object from the given results and criteria
     *
     * @param searchResults    File bean search results
     * @param resultProperties Search result property map
     * @param buildName        Build name to search for
     * @param buildNumber      Build number to search for
     * @return The file info of a result that best matches the given criteria
     */
    FileInfo getBestMatchingResult(List<RepoPath> searchResults, Map<RepoPath, Properties> resultProperties,
            String buildName, long buildNumber);
}