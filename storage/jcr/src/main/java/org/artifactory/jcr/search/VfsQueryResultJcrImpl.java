package org.artifactory.jcr.search;

import com.google.common.collect.Lists;
import org.artifactory.jcr.data.VfsNodeJcrImpl;
import org.artifactory.jcr.utils.JcrHelper;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.search.VfsQueryResult;
import org.artifactory.sapi.search.VfsQueryRow;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import java.util.Iterator;
import java.util.List;

/**
 * Date: 8/5/11
 * Time: 7:15 PM
 *
 * @author Fred Simon
 */
public class VfsQueryResultJcrImpl implements VfsQueryResult {
    private final QueryResult queryResult;
    private long nbResult;

    public VfsQueryResultJcrImpl(QueryResult queryResult) {
        this.queryResult = queryResult;
    }

    public Iterable<VfsNode> getNodes() {
        try {
            List<VfsNode> result = Lists.newArrayList();
            NodeIterator nodes = queryResult.getNodes();
            while (nodes.hasNext()) {
                try {
                    result.add(new VfsNodeJcrImpl(nodes.nextNode()));
                } catch (RepositoryRuntimeException e) {
                    JcrHelper.handleNotFoundException(e);
                }
                nbResult++;
            }
            return result;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public Iterator<VfsQueryRow> rowsIterator() {
        final RowIterator rows;
        try {
            rows = queryResult.getRows();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        return new Iterator<VfsQueryRow>() {
            public boolean hasNext() {
                return rows.hasNext();
            }

            public VfsQueryRow next() {
                return new VfsQueryRowJcrImpl(rows.nextRow());
            }

            public void remove() {
                throw new UnsupportedOperationException("Cannot remove elements from a row result iterator");
            }
        };
    }

    public long getCount() {
        return nbResult;
    }
}
