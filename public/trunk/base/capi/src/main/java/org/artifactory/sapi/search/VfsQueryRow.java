package org.artifactory.sapi.search;

/**
 * Date: 8/5/11
 * Time: 10:52 PM
 *
 * @author Fred Simon
 */
public interface VfsQueryRow {
    String nodeAbsolutePath();

    String excerpt(String propertyName);
}
