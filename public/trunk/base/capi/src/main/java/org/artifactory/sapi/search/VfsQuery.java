package org.artifactory.sapi.search;

import org.artifactory.sapi.data.VfsNodeType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Calendar;

/**
 * Date: 8/5/11
 * Time: 6:04 PM
 *
 * @author Fred Simon
 */
public interface VfsQuery {
    void setNodeTypeFilter(@Nonnull VfsNodeType nodeType);

    VfsQueryCriterion addCriterion(@Nonnull String propertyName, @Nonnull VfsComparatorType comparator,
            @Nullable String value);

    VfsQueryCriterion addCriterion(@Nonnull String propertyName, @Nonnull VfsComparatorType comparator,
            @Nonnull VfsFunctionType function, @Nullable String value);

    VfsQueryCriterion addCriterion(@Nonnull String propertyName, @Nonnull VfsComparatorType comparator,
            @Nullable Long value);

    VfsQueryCriterion addCriterion(@Nonnull String propertyName, @Nonnull VfsComparatorType comparator,
            @Nullable Calendar value);

    void orderByAscending(@Nonnull String propertyName);

    void orderByDescending(@Nonnull String propertyName);

    @Nonnull
    VfsQueryResult execute(boolean limit);

    VfsQueryCriterion addSmartEqualCriterion(@Nonnull String propertyName, @Nullable String value);

    VfsQueryCriterion group(VfsQueryCriterion... vfsQueryCriterion);

    void setRootPath(String rootPath);

    void addAllSubPathFilter();

    VfsQueryPathCriterion addRelativePathFilter(String pathSearch);

    VfsQueryPathCriterion addPathFilters(String... pathFilters);

    VfsQueryPathCriterion addPathFilter(String pathFilter);

    VfsQueryPathCriterion addMetadataNameFilter(String pathFilter);
}
