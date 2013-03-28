package org.artifactory.jcr.search;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ISO9075;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.sapi.search.InvalidQueryRuntimeException;
import org.artifactory.sapi.search.VfsBoolType;
import org.artifactory.sapi.search.VfsRepoQuery;

import java.util.Collection;
import java.util.Set;

/**
 * Date: 8/5/11
 * Time: 9:30 PM
 *
 * @author Fred Simon
 */
public class VfsRepoQueryJcrImpl extends VfsQueryJcrImpl implements VfsRepoQuery {
    private final Set<String> repoKeys = Sets.newHashSet();

    public VfsRepoQueryJcrImpl() {
        setRootPath(PathFactoryHolder.get().getAllRepoRootPath());
    }

    @Override
    public void setSingleRepoKey(String repoKey) {
        this.repoKeys.clear();
        this.repoKeys.add(repoKey);
    }

    @Override
    public void setRepoKeys(Collection<String> repoKeys) {
        this.repoKeys.clear();
        this.repoKeys.addAll(repoKeys);
    }

    @Override
    protected void fillBase(StringBuilder query) {
        super.fillBase(query);
        if (!repoKeys.isEmpty()) {
            if (repoKeys.size() == 1) {
                // Simple path
                String simplePath = repoKeys.iterator().next();
                if (!StringUtils.isBlank(simplePath)) {
                    query.append(ISO9075.encodePath(simplePath));
                } else {
                    throw new InvalidQueryRuntimeException("Cannot accept null or empty repo key!");
                }
            } else {
                query.append(". [");
                VfsBoolType nextBool = null;
                for (String repoKey : repoKeys) {
                    if (nextBool != null) {
                        query.append(" ").append(nextBool.str).append(" ");
                    }
                    query.append("fn:name() = '").append(ISO9075.encodePath(repoKey)).append("'");
                    nextBool = VfsBoolType.OR;
                }
                query.append("]");
            }
            JcrQueryHelper.addSlashIfNeeded(query);
        } else {
            // If no repo add the /*/
            query.append("*").append(JcrQueryHelper.SLASH_CHAR);
        }
    }
}
