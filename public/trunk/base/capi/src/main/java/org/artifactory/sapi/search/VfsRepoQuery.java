package org.artifactory.sapi.search;

import java.util.Collection;

/**
 * Date: 8/5/11
 * Time: 9:34 PM
 *
 * @author Fred Simon
 */
public interface VfsRepoQuery extends VfsQuery {
    void setSingleRepoKey(String repoKey);

    void setRepoKeys(Collection<String> repoKeys);
}
