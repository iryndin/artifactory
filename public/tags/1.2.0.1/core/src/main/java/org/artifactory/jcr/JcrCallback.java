package org.artifactory.jcr;

import javax.jcr.RepositoryException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface JcrCallback<T> {

    T doInJcr(JcrSessionWrapper session) throws RepositoryException;
}
