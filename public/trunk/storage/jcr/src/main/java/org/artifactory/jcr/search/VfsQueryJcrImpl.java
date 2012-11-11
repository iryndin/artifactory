package org.artifactory.jcr.search;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.sapi.data.VfsNodeType;
import org.artifactory.sapi.search.InvalidQueryRuntimeException;
import org.artifactory.sapi.search.VfsComparatorType;
import org.artifactory.sapi.search.VfsFunctionType;
import org.artifactory.sapi.search.VfsQuery;
import org.artifactory.sapi.search.VfsQueryCriterion;
import org.artifactory.sapi.search.VfsQueryPathCriterion;
import org.artifactory.sapi.search.VfsQueryResult;
import org.artifactory.util.PathUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.query.QueryResult;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;

import static org.artifactory.sapi.search.VfsQueryPathCriterion.ALL_PATH_VALUE;

/**
 * Date: 8/5/11
 * Time: 6:07 PM
 *
 * @author Fred Simon
 */
public class VfsQueryJcrImpl implements VfsQuery {

    private final EnumSet<VfsNodeType> matchNodeTypes = EnumSet.noneOf(VfsNodeType.class);

    private final BaseGroupCriterion defaultGroup;

    private final List<OrderBy> orders = Lists.newArrayList();
    protected String rootPath;
    protected final List<BaseVfsQueryCriterion> pathCriteria = Lists.newArrayList();

    public VfsQueryJcrImpl() {
        defaultGroup = new BaseGroupCriterion(this);
    }

    @Override
    public void setRootPath(String rootPath) {
        this.rootPath = PathUtils.trimSlashes(rootPath).toString();
    }

    @Override
    public void setNodeTypeFilter(@Nonnull VfsNodeType nodeType) {
        matchNodeTypes.add(nodeType);
    }

    @Override
    public VfsQueryCriterion addSmartEqualCriterion(@Nonnull String propertyName, @Nullable String value) {
        return internalAddCriterion(createSmartEqualCriterion(propertyName, value));
    }

    @Override
    public VfsQueryCriterion addCriterion(@Nonnull String propertyName, @Nonnull VfsComparatorType comparator,
            @Nullable String value) {
        return internalAddCriterion(new VfsQueryCriterionJcrImpl(propertyName, comparator, value));
    }

    @Override
    public VfsQueryCriterion addCriterion(@Nonnull String propertyName, @Nonnull VfsComparatorType comparator,
            @Nonnull VfsFunctionType function, @Nullable String value) {
        return internalAddCriterion(new VfsQueryCriterionJcrImpl(propertyName, comparator, function, value));
    }

    @Override
    public VfsQueryCriterion addCriterion(@Nonnull String propertyName, @Nonnull VfsComparatorType comparator,
            @Nullable Long value) {
        return internalAddCriterion(new VfsQueryCriterionJcrImpl(propertyName, value, comparator));
    }

    @Override
    public VfsQueryCriterion addCriterion(@Nonnull String propertyName, @Nonnull VfsComparatorType comparator,
            @Nullable Calendar value) {
        return internalAddCriterion(new VfsQueryCriterionJcrImpl(propertyName, value, comparator));
    }

    @Override
    public VfsQueryCriterion group(VfsQueryCriterion... vfsQueryCriteria) {
        return defaultGroup.group(vfsQueryCriteria);
    }

    @Override
    public void orderByAscending(@Nonnull String propertyName) {
        orders.add(new OrderBy(propertyName, true));
    }

    @Override
    public void orderByDescending(@Nonnull String propertyName) {
        orders.add(new OrderBy(propertyName, false));
    }

    private BaseVfsQueryCriterion internalAddCriterion(BaseVfsQueryCriterion criterion) {
        return defaultGroup.addCriterion(criterion);
    }

    @Override
    @Nonnull
    public VfsQueryResult execute(boolean limit) {
        StringBuilder query = new StringBuilder();
        fillBase(query);
        fillPathCriterion(query);
        fillNodeTypes(query);
        fillCriteria(query);
        fillOrderBy(query);
        JcrService jcr = StorageContextHelper.get().getJcrService();
        JcrQuerySpec spec = JcrQuerySpec.xpath(query.toString());
        if (!limit) {
            spec.noLimit();
        }
        QueryResult queryResult = jcr.executeQuery(spec);
        return new VfsQueryResultJcrImpl(queryResult);
    }

    protected void fillOrderBy(StringBuilder query) {
        for (OrderBy order : orders) {
            query.append("order by @").append(order.propertyName);
            if (order.ascending) {
                query.append(" ascending");
            } else {
                query.append(" descending");
            }
        }
    }

    protected void fillNodeTypes(StringBuilder query) {
        if (!this.matchNodeTypes.isEmpty()) {
            if (matchNodeTypes.size() > 1) {
                throw new InvalidQueryRuntimeException("No yet support for fetching multiple types in one query");
            }
            JcrQueryHelper.addSlashIfNeeded(query);
            for (VfsNodeType matchNodeType : matchNodeTypes) {
                JcrQueryHelper.fillWithNodeTypeFilter(query, matchNodeType);
            }
        }
    }

    protected void fillCriteria(StringBuilder query) {
        defaultGroup.fill(query);
    }

    protected void fillBase(StringBuilder query) {
        query.append("/jcr:root/");
        if (!StringUtils.isBlank(rootPath)) {
            query.append(rootPath).append(JcrQueryHelper.SLASH_CHAR);
        }
    }

    protected VfsQueryCriterionJcrImpl createSmartEqualCriterion(String propertyName, String value) {
        VfsComparatorType type;
        if (value == null || StringUtils.isBlank(value)) {
            type = VfsComparatorType.ANY;
        } else {
            if (value.contains("*") || value.contains("?")) {
                type = VfsComparatorType.CONTAINS;
            } else {
                type = VfsComparatorType.EQUAL;
            }
        }
        return new VfsQueryCriterionJcrImpl(propertyName, type, value);
    }

    @Override
    public void addAllSubPathFilter() {
        addPathFilter(ALL_PATH_VALUE);
    }

    @Override
    public VfsQueryPathCriterion addPathFilter(String pathFilter) {
        return internalAddPathCriterion(createSmartPathCriterion(pathFilter, null));
    }

    @Override
    public VfsQueryPathCriterion addMetadataNameFilter(String pathFilter) {
        return internalAddPathCriterion(createSmartPathCriterion(pathFilter, VfsNodeType.METADATA));
    }

    @Override
    public VfsQueryPathCriterion addPathFilters(String... pathFilters) {
        VfsQueryPathCriterion pathCriterion = null;
        for (String pathFilter : pathFilters) {
            if (StringUtils.isBlank(pathFilter)) {
                pathCriterion = internalAddPathCriterion(createSmartPathCriterion("*", null));
            } else {
                pathCriterion = internalAddPathCriterion(createSmartPathCriterion(pathFilter, null));
            }
        }
        return pathCriterion;
    }

    @Override
    public VfsQueryPathCriterion addRelativePathFilter(String relativePathFilter) {
        // We can have double // in this path,
        // Be careful when we split with slash // will return an empty string
        // IMPORTANT NOTE... Big bug in spilt, splitting "//g1*//" retun {"", "", "g1*"} instead of {"", "g1*", ""}
        String[] split = relativePathFilter.split(JcrQueryHelper.FORWARD_SLASH);
        VfsQueryPathCriterion pathCriterion = null;
        for (String path : split) {
            pathCriterion = internalAddPathCriterion(createSmartPathCriterion(path, null));
        }
        return pathCriterion;
    }

    private VfsQueryPathCriterionJcrImpl createSmartPathCriterion(String pathFilter, @Nullable VfsNodeType nodeType) {
        if (pathFilter == null || pathFilter.contains(JcrQueryHelper.FORWARD_SLASH)) {
            throw new InvalidQueryRuntimeException(
                    "Path filter element cannot be null or contain slash: " + pathFilter);
        }

        if (pathFilter.length() == 0 || ALL_PATH_VALUE.equals(pathFilter)) {
            return new VfsQueryPathCriterionJcrImpl(VfsComparatorType.ANY, ALL_PATH_VALUE, nodeType);
        } else {
            if (pathFilter.contains("*") || pathFilter.contains("?")) {
                return new VfsQueryPathCriterionJcrImpl(VfsComparatorType.CONTAINS, pathFilter, nodeType);
            } else {
                return new VfsQueryPathCriterionJcrImpl(VfsComparatorType.EQUAL, pathFilter, nodeType);
            }
        }
    }

    private VfsQueryPathCriterion internalAddPathCriterion(VfsQueryPathCriterionJcrImpl criterion) {
        if (criterion != null && criterion.isValid()) {
            pathCriteria.add(criterion);
            return criterion;
        }
        return null;
    }

    protected void fillPathCriterion(StringBuilder query) {
        if (!pathCriteria.isEmpty()) {
            for (BaseVfsQueryCriterion criteria : pathCriteria) {
                criteria.fill(query);
            }
        }
    }

    static class OrderBy {
        String propertyName;
        boolean ascending;

        OrderBy(String propertyName, boolean ascending) {
            this.propertyName = propertyName;
            this.ascending = ascending;
        }
    }

}
