package org.artifactory.jcr.search;

import org.apache.jackrabbit.JcrConstants;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.search.VfsQueryRow;

import javax.jcr.RepositoryException;
import javax.jcr.query.Row;

/**
 * Date: 8/5/11
 * Time: 10:55 PM
 *
 * @author Fred Simon
 */
public class VfsQueryRowJcrImpl implements VfsQueryRow {
    private final Row row;

    public VfsQueryRowJcrImpl(Row row) {
        this.row = row;
    }

    public String nodeAbsolutePath() {
        try {
            return row.getValue(JcrConstants.JCR_PATH).getString();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public String excerpt(String propertyName) {
        try {
            return row.getValue("rep:excerpt(" + propertyName + ")").getString();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}
