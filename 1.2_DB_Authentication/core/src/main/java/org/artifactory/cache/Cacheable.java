package org.artifactory.cache;

import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface Cacheable {
    Date getLastUpdated();

    /**
     * Returns the age of a cached resource in millis or -1 if the resource is not cached
     * @return
     */
    long getAge();
}
