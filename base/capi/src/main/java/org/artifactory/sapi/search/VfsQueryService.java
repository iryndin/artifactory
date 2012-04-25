package org.artifactory.sapi.search;

import org.artifactory.sapi.common.RequiresTransaction;
import org.artifactory.sapi.common.TxPropagation;

/**
 * Date: 8/5/11
 * Time: 9:39 PM
 *
 * @author Fred Simon
 */
@RequiresTransaction(TxPropagation.SUPPORTS)
public interface VfsQueryService {
    VfsQuery createQuery();

    VfsRepoQuery createRepoQuery();
}
