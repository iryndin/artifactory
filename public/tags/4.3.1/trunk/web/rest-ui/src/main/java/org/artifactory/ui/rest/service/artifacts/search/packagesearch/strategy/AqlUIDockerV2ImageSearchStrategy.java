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
public class AqlUIDockerV2ImageSearchStrategy extends AqlUIPropertySearchStrategy {

    public AqlUIDockerV2ImageSearchStrategy(String key, AqlDomainEnum[] subdomainPath) {
        super(key, subdomainPath);
    }

    @Override
    public AqlBase.OrClause toQuery() {
        AqlBase.OrClause query = AqlApiItem.or();
        values.stream().forEach(value -> {
            AqlBase.AndClause pathAnd = AqlApiItem.and();
            AqlBase.OrClause propOr = AqlApiItem.or();
            //Don't search for docker v1 tags that have the same property on a different json descriptor
            pathAnd.append(new AqlBase.CriteriaClause(AqlFieldEnum.itemName, Lists.newArrayList(AqlDomainEnum.items),
                    AqlComparatorEnum.equals, "manifest.json"));
            if (value.endsWith("*")) {
                propOr.append(
                        new AqlBase.PropertyCriteriaClause(key, AqlComparatorEnum.matches, value, subdomainPath));
                propOr.append(
                        new AqlBase.PropertyCriteriaClause(key, AqlComparatorEnum.matches, "*" + value, subdomainPath));
            } else {
                propOr.append(
                        new AqlBase.PropertyCriteriaClause(key, AqlComparatorEnum.matches, value, subdomainPath));
                propOr.append(
                        new AqlBase.PropertyCriteriaClause(key, AqlComparatorEnum.matches, "library/" + value, subdomainPath));
            }
            pathAnd.append(propOr);
            query.append(pathAnd);
        });
        return query;
    }

    @Override
    public boolean includePropsInResult() {
        return true;
    }
}