package org.artifactory.jcr.search;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.util.Text;
import org.artifactory.sapi.search.VfsBoolType;
import org.artifactory.sapi.search.VfsComparatorType;
import org.artifactory.sapi.search.VfsFunctionType;
import org.artifactory.sapi.search.VfsQueryCriterion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Calendar;

/**
 * Date: 8/6/11
 * Time: 12:01 PM
 *
 * @author Fred Simon
 */
class VfsQueryCriterionJcrImpl extends BaseVfsQueryCriterion {
    final String propertyName;
    final VfsComparatorType comparator;
    String value;
    String[] propertySubPath;
    VfsFunctionType function = VfsFunctionType.NONE;

    VfsQueryCriterionJcrImpl(String propertyName, VfsComparatorType comparator) {
        super();
        this.propertyName = propertyName;
        this.comparator = comparator;
    }

    VfsQueryCriterionJcrImpl(@Nonnull String propertyName, @Nonnull VfsComparatorType comparator,
            @Nullable String value) {
        this(propertyName, comparator);
        if (!StringUtils.isBlank(value)) {
            if (VfsComparatorType.CONTAINS.equals(comparator)) {
                this.value = "'" + value + "'";
            } else {
                this.value = "'" + Text.escapeIllegalXpathSearchChars(value) + "'";
            }
        }
    }

    VfsQueryCriterionJcrImpl(@Nonnull String propertyName, @Nullable Calendar value,
            @Nonnull VfsComparatorType comparator) {
        this(propertyName, comparator);
        if (value != null) {
            this.value = "xs:dateTime('" + ISO8601.format(value) + "')";
        }
    }

    VfsQueryCriterionJcrImpl(@Nonnull String propertyName, @Nullable Long value,
            @Nonnull VfsComparatorType comparator) {
        this(propertyName, comparator);
        if (value != null) {
            this.value = value.toString();
        }
    }

    public VfsQueryCriterionJcrImpl(@Nonnull String propertyName, @Nonnull VfsComparatorType comparator,
            @Nonnull VfsFunctionType function, @Nullable String value) {
        this(propertyName, comparator, value);
        this.function = function;
    }

    @Override
    public VfsQueryCriterion addPropertySubPath(String... pathElements) {
        propertySubPath = pathElements;
        return this;
    }

    @Override
    public boolean isValid() {
        return !StringUtils.isEmpty(value)
                || comparator == VfsComparatorType.NONE
                || comparator == VfsComparatorType.ANY;
    }

    protected void fillPropertyName(StringBuilder query) {
        if (propertySubPath != null && propertySubPath.length > 0) {
            for (String pathEl : propertySubPath) {
                query.append(pathEl).append("/");
            }
        }
        query.append("@").append(propertyName);
    }

    @Override
    protected VfsBoolType fill(StringBuilder query) {
        if (comparator == VfsComparatorType.ANY) {
            fillPropertyName(query);
        } else if (comparator == VfsComparatorType.NONE) {
            query.append("not(");
            fillPropertyName(query);
            query.append(")");
        } else if (comparator == VfsComparatorType.CONTAINS) {
            query.append("jcr:contains(");
            fillPropertyName(query);
            query.append(", ").append(value).append(")");
        } else {
            boolean applyFunction = VfsFunctionType.NONE != function;
            if (applyFunction) {
                query.append("fn:").append(function.str).append("(");
            }
            fillPropertyName(query);
            if (applyFunction) {
                query.append(")");
            }
            query.append(" ").append(comparator.str).append(" ");
            if (!StringUtils.isEmpty(value)) {
                query.append(value);
            }
        }
        return nextBool;
    }
}
