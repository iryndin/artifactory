package org.artifactory.ui.rest.service.artifacts.search.packagesearch.strategy;

import com.google.common.collect.Lists;
import org.artifactory.aql.api.domain.sensitive.AqlApiItem;
import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlFieldEnum;

/**
 * @author Dan Feldman
 */
public class AqlUIDebianNameSearchStrategy extends AqlUIFieldSearchStrategy {

    public AqlUIDebianNameSearchStrategy(AqlFieldEnum field, AqlDomainEnum[] subdomainPath) {
        super(field, subdomainPath);
    }

    @Override
    public AqlBase.OrClause toQuery() {
        AqlBase.OrClause query = AqlApiItem.or();
        values.stream().forEach(value -> {
            StringBuilder nameMatcher = new StringBuilder();
            if (value.contains("*") || value.contains("?")) {
                nameMatcher.append(value);
            } else {
                nameMatcher.append("*").append(value).append("*");
            }
            if (!value.contains(".deb")) {
                nameMatcher.append(".deb");
            }
            query.append(new AqlBase.CriteriaClause(field, Lists.newArrayList(AqlDomainEnum.items),
                    AqlComparatorEnum.matches, nameMatcher.toString()));
        });
        return query;
    }

    @Override
    public boolean includePropsInResult() {
        return true;
    }
}