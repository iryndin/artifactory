package org.artifactory.repo.interceptor;

import org.artifactory.jcr.JcrFile;
import org.artifactory.resource.RepoResource;

import java.io.InputStream;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public interface LocalRepoInterceptor extends Serializable {
    InputStream beforeSaveResource(RepoResource res, InputStream in) throws Exception;

    void afterSaveResource(RepoResource res, JcrFile file) throws Exception;
}
