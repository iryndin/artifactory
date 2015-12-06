package org.artifactory.ui.rest.service.artifacts.search.packagesearch.strategy;

import org.artifactory.aql.api.domain.sensitive.AqlApiItem;
import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;

/**
 * @author Dan Feldman
 */
public class AqlUINpmNameSearchStrategy extends AqlUIPropertySearchStrategy {

    public AqlUINpmNameSearchStrategy(String key, AqlDomainEnum[] subdomainPath) {
        super(key, subdomainPath);
    }

    @Override
    public AqlBase.OrClause toQuery() {
        AqlBase.OrClause query = AqlApiItem.or();
        values.stream().forEach(value -> {
                    // TODO: [by dan] Changed to forced matches comparison due to Derby taking exponentially longer times to do it with equals
                    query.append(new AqlBase.PropertyCriteriaClause(key, AqlComparatorEnum.matches, value, subdomainPath));
                    //match scoped packages
                    query.append(new AqlBase.PropertyCriteriaClause(key, AqlComparatorEnum.matches, "@*" + value,
                            subdomainPath));
                }
        );
        return query;
    }

    @Override
    public boolean includePropsInResult() {
        return true;
    }
}