package org.artifactory.jcr;

import org.artifactory.jcr.fs.JcrFile;

/**
 * Date: 8/4/11
 * Time: 8:57 PM
 *
 * @author Fred Simon
 */
public interface JcrSearchService {
    boolean markArchiveForIndexing(JcrFile newJcrFile, boolean force);
}
