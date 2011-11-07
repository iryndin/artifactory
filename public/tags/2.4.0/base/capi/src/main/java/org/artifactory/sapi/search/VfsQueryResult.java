package org.artifactory.sapi.search;

import org.artifactory.sapi.data.VfsNode;

import java.util.Iterator;

/**
 * Date: 8/5/11
 * Time: 6:47 PM
 *
 * @author Fred Simon
 */
public interface VfsQueryResult {
    long getCount();

    Iterable<VfsNode> getNodes();

    Iterator<VfsQueryRow> rowsIterator();
}
