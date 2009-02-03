package org.artifactory.resource;

import org.artifactory.cache.Cacheable;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface RepoResource extends Cacheable, Serializable {
    String NA = "NA";

    String getRelPath();

    String getRelDirPath();

    String getName();

    Date getLastModified();

    long getLastModifiedTime();

    String getRepoKey();

    long getSize();

    boolean isFound();

    String getAbsPath();
}
