package org.artifactory.repo.interceptor;

import org.artifactory.repo.LocalRepo;
import org.artifactory.resource.RepoResource;

import java.io.InputStream;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public interface LocalRepoInterceptor extends Serializable {
    InputStream beforeResourceSave(RepoResource res, LocalRepo repo, InputStream in)
            throws Exception;

    void afterResourceSave(RepoResource res, LocalRepo repo) throws Exception;
}
