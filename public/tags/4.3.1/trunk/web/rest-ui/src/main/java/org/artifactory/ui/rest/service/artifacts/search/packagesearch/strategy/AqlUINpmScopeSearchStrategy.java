package org.artifactory.ui.rest.service.artifacts.search.packagesearch.strategy;

import org.artifactory.aql.api.domain.sensitive.AqlApiItem;
import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;

/**
 * @author Dan Feldman
 */
public class AqlUINpmScopeSearchStrategy extends AqlUIPropertySearchStrategy {

    public AqlUINpmScopeSearchStrategy(String key, AqlDomainEnum[] subdomainPath) {
        super(key, subdomainPath);
    }

    @Override
    public AqlBase.OrClause toQuery() {
        AqlBase.OrClause query = AqlApiItem.or();
        values.stream().forEach(value -> {
            if(value.contains("@")) {
                query.append(
                        new AqlBase.PropertyCriteriaClause(key, AqlComparatorEnum.matches, value + "/*", subdomainPath));
            } else {
                query.append(
                        new AqlBase.PropertyCriteriaClause(key, AqlComparatorEnum.matches, "@" + value + "/*", subdomainPath));
            }
        });
        return query;
    }

    @Override
    public boolean includePropsInResult() {
        return true;
    }
}