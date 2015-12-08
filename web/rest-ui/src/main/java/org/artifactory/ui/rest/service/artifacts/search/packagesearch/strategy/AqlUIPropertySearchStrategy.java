package org.artifactory.ui.rest.service.artifacts.search.packagesearch.strategy;

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
public class AqlUIPropertySearchStrategy implements AqlUISearchStrategy {

    protected String key;
    protected List<String> values;
    protected List<AqlDomainEnum> subdomainPath;
    protected AqlComparatorEnum comparator;


    public AqlUIPropertySearchStrategy(String key, AqlDomainEnum[] subdomainPath) {
        this.key = key;
        this.subdomainPath = Stream.of(subdomainPath).collect(Collectors.toList());
    }

    public AqlUIPropertySearchStrategy(String key, List<AqlDomainEnum> subdomainPath) {
        this.key = key;
        this.subdomainPath = subdomainPath;
    }

    @Override
    public AqlUIPropertySearchStrategy values(List<String> values) {
        this.values = values;
        return this;
    }

    @Override
    public AqlUIPropertySearchStrategy values(String... values) {
        this.values = Stream.of(values).collect(Collectors.toList());
        return this;
    }

    @Override
    public AqlUIPropertySearchStrategy comparator(AqlComparatorEnum comparator) {
        this.comparator = comparator;
        return this;
    }

    @Override
    public AqlFieldEnum getSearchField() {
        return AqlFieldEnum.propertyKey;
    }

    @Override
    public String getSearchKey() {
        return key;
    }

    @Override
    // TODO: [by dan] Changed to forced matches comparison due to Derby taking exponentially longer times to do it with equals
    public AqlBase.OrClause toQuery() {
        AqlBase.OrClause query = AqlApiItem.or();
        values.stream().forEach(value ->
                query.append(new AqlBase.PropertyCriteriaClause(key, AqlComparatorEnum.matches, value, subdomainPath)));
        return query;
    }

    @Override
    public boolean includePropsInResult() {
        return true;
    }

    @Override
    public String toString() {
        return "AqlUIPropertySearchStrategy{" +
                "property key: " + key +
                ", values: " + PathUtils.collectionToDelimitedString(values) +
                ", comparator: " + comparator +
                '}';
    }
}