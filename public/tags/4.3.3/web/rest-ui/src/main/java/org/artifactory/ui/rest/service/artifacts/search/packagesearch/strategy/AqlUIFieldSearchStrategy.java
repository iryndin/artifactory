package org.artifactory.ui.rest.service.artifacts.search.packagesearch.strategy;

import com.google.common.collect.Lists;
import org.artifactory.aql.api.domain.sensitive.AqlApiItem;
import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.util.PathUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Dan Feldman
 */
public class AqlUIFieldSearchStrategy implements AqlUISearchStrategy {

    protected AqlFieldEnum field;
    protected List<String> values;
    protected List<AqlDomainEnum> subdomainPath;
    protected AqlComparatorEnum comparator;

    public AqlUIFieldSearchStrategy(AqlFieldEnum field, AqlDomainEnum[] subdomainPath) {
        this.field = field;
        this.subdomainPath = Stream.of(subdomainPath).collect(Collectors.toList());
    }

    public AqlUIFieldSearchStrategy(AqlFieldEnum field, List<AqlDomainEnum> subdomainPath) {
        this.field = field;
        this.subdomainPath = subdomainPath;
    }

    @Override
    public AqlUIFieldSearchStrategy values(List<String> values) {
        this.values = values;
        return this;
    }

    @Override
    public AqlUIFieldSearchStrategy values(String... values) {
        this.values = Stream.of(values).collect(Collectors.toList());
        return this;
    }

    @Override
    public AqlUIFieldSearchStrategy comparator(AqlComparatorEnum comparator) {
        this.comparator = comparator;
        return this;
    }

    @Override
    public AqlFieldEnum getSearchField() {
        return field;
    }

    @Override
    public String getSearchKey() {
        return "";
    }

    @Override
    public AqlBase.OrClause toQuery() {
        AqlBase.OrClause query = AqlApiItem.or();
        values.stream().forEach(value -> query.append(
                new AqlBase.CriteriaClause(field, Lists.newArrayList(AqlDomainEnum.items), comparator, value)));
        return query;
    }

    @Override
    public boolean includePropsInResult() {
        return false;
    }

    @Override
    public String toString() {
        return "AqlUIFieldSearchStrategy{" +
                "field: " + field +
                ", values: " + PathUtils.collectionToDelimitedString(values) +
                ", comparator: " + comparator +
                '}';
    }
}
