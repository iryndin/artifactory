package org.artifactory.ui.rest.service.artifacts.search.packagesearch.strategy;

import org.artifactory.aql.api.domain.sensitive.AqlApiItem;
import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;

/**
 * @author Dan Feldman
 */
public class AqlUIDockerV2ImageDigestSearchStrategy extends AqlUIPropertySearchStrategy {

    public AqlUIDockerV2ImageDigestSearchStrategy(String key, AqlDomainEnum[] subdomainPath) {
        super(key, subdomainPath);
    }

    @Override
    public AqlBase.OrClause toQuery() {
        AqlBase.OrClause query = AqlApiItem.or();
        values.stream().forEach(value -> {
            AqlBase.AndClause and = AqlApiItem.and();
            //The image digest
            and.append(new AqlBase.PropertyCriteriaClause(key, comparator, value, subdomainPath));
            //Don't return other sha256 tagged artifacts - only point to the tag's manifest.json which must have a
            // value in the docker.repoName and docker.manifest props (to make sure it's v2)
            and.append(new AqlBase.PropertyCriteriaClause("docker.repoName", AqlComparatorEnum.matches, "*",
                    subdomainPath));
            and.append(new AqlBase.PropertyCriteriaClause("docker.manifest", AqlComparatorEnum.matches, "*",
                    subdomainPath));
            query.append(and);
        });
        return query;
    }

    @Override
    public boolean includePropsInResult() {
        return true;
    }
}