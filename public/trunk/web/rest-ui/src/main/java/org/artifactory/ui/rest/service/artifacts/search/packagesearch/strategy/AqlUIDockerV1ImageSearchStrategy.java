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
public class AqlUIDockerV1ImageSearchStrategy extends AqlUIFieldSearchStrategy {

    public AqlUIDockerV1ImageSearchStrategy(AqlFieldEnum field, AqlDomainEnum[] subdomainPath) {
        super(field, subdomainPath);
    }

    @Override
    public AqlBase.OrClause toQuery() {
        AqlBase.OrClause query = AqlApiItem.or();
        values.stream().forEach(value -> {
            AqlBase.AndClause mainAnd = AqlBase.and();
            // adds a clause to the main or with library/value to try and match standard docker hub images.
            AqlBase.AndClause libMatchAnd = AqlBase.and();
            StringBuilder libMatcher = null;
            if (!value.contains("library/")) {
                libMatcher = new StringBuilder("repositories/library/");
            }

            StringBuilder nameMatcher = new StringBuilder("repositories/");
            if (value.contains("*") || value.contains("?")) {
                nameMatcher.append(value);
                if (libMatcher != null) {
                    libMatcher.append(value);
                }
            } else {
                nameMatcher.append("*").append(value).append("*");
                if (libMatcher != null) {
                    libMatcher.append("*").append(value).append("*");
                }
            }

            if (libMatcher != null) {
                libMatcher.append("/**");
                libMatchAnd.append(new AqlBase.CriteriaClause(field, Lists.newArrayList(AqlDomainEnum.items),
                        AqlComparatorEnum.matches, libMatcher.toString()));
                libMatchAnd.append(
                        new AqlBase.CriteriaClause(AqlFieldEnum.itemName, Lists.newArrayList(AqlDomainEnum.items),
                                AqlComparatorEnum.equals, "tag.json"));
                query.append(libMatchAnd);
            }

            nameMatcher.append("/**");
            mainAnd.append(new AqlBase.CriteriaClause(field, Lists.newArrayList(AqlDomainEnum.items),
                    AqlComparatorEnum.matches, nameMatcher.toString()));
            mainAnd.append(new AqlBase.CriteriaClause(AqlFieldEnum.itemName, Lists.newArrayList(AqlDomainEnum.items),
                    AqlComparatorEnum.equals, "tag.json"));

            query.append(mainAnd);
        });
        return query;
    }

    @Override
    public boolean includePropsInResult() {
        return true;
    }
}