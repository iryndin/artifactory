package org.artifactory.ui.rest.service.artifacts.search.packagesearch.util;

import org.artifactory.aql.api.domain.sensitive.AqlApiItem;
import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.aql.result.rows.FullRow;
import org.artifactory.common.ConstantValues;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.criteria.FieldSearchCriteria;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.criteria.PackageSearchCriteria;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.criteria.PackageSearchCriteria.PackageSearchType;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.search.AqlUISearchModel;
import org.artifactory.ui.rest.service.artifacts.search.packagesearch.strategy.AqlUIPropertySearchStrategy;
import org.artifactory.ui.rest.service.artifacts.search.packagesearch.strategy.AqlUISearchStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Dan Feldman
 */
public class PackageSearchHelper {
    private static final Logger log = LoggerFactory.getLogger(PackageSearchHelper.class);

    /**
     * Builds an AQL query from the given search models which are constructed when a criteria is sent by the UI.
     */
    public static AqlBase buildItemQuery(List<AqlUISearchModel> searches, boolean includePropertiesInResult
            /*, boolean includeExtraFieldsInResult*/) {
        List<AqlUISearchStrategy> strategies = buildStrategiesFromSearchModel(searches);
        log.debug("input searches resolved to the following strategies: " + Arrays.toString(strategies.toArray()));

        AqlApiItem.AndClause query = AqlApiItem.and();
        strategies.stream()
                .map(AqlUISearchStrategy::toQuery)
                .filter(orClause -> !orClause.isEmpty())
                .forEach(query::append);

        int noOfPropKeys = 1;
        AqlApiItem.OrClause propKeyIncluder = AqlApiItem.or();
        if (includePropertiesInResult) {
            noOfPropKeys = populatePropKeysToInclude(searches, propKeyIncluder);
            AqlBase.PropertyResultFilterClause<AqlBase> resultFilter = AqlApiItem.propertyResultFilter();
            if (!propKeyIncluder.isEmpty()) {
                resultFilter.append(propKeyIncluder);
                query.append(resultFilter);
            }
        }
        AqlApiItem aql = AqlApiItem.create().filter(query);
        //Result set size limit is calculated by (max ui results) * no of property keys being searched
        // == UI result limit (i.e. 500)repo paths + all of the required properties that will be shown in UI.
        log.debug("Total number of props for current package type being searched is {}, result set limit is set to {}",
                noOfPropKeys, ConstantValues.searchMaxResults.getInt() * noOfPropKeys);
        aql.limit(ConstantValues.searchMaxResults.getInt() * noOfPropKeys);
        if (includePropertiesInResult && !propKeyIncluder.isEmpty()) {
            aql.include(AqlApiItem.property().key(), AqlApiItem.property().value());
        }
        //includeExtraFieldsInResultIfNeeded(strategies, aql, includeExtraFieldsInResult); // TODO: [by dan] see below
        return aql;
    }

    private static int populatePropKeysToInclude(List<AqlUISearchModel> searches, AqlBase.OrClause propKeyIncluder) {
        int noOfPropKeys;//Add all of the Package Type's prop keys to the result filter
        List<AqlBase.CriteriaClause<AqlApiItem>> propKeyIncludes = searches.stream()
                .map(PackageSearchHelper::getPackageSearchTypeBySearchModel)
                .filter(packageSearchType -> packageSearchType != null)
                .map(PackageSearchHelper::getStrategiesForPackageType)
                .map(PackageSearchHelper::getPropKeysFromStrategies)
                .flatMap(Collection::stream)
                .distinct()
                .map(key -> AqlApiItem.property().key().equal(key))
                .collect(Collectors.toList());

        noOfPropKeys = propKeyIncludes.isEmpty() ? 1 : propKeyIncludes.size();
        propKeyIncludes.stream()
                .forEach(propKeyIncluder::append);
        return noOfPropKeys;
    }

    public static PackageSearchType getPackageSearchTypeBySearchModel(AqlUISearchModel model) {
        return PackageSearchCriteria.getPackageTypeByFieldId(model.getId());
    }

    public static List<AqlUISearchStrategy> getStrategiesForPackageType(PackageSearchType type) {
        return PackageSearchCriteria.getStartegiesByPackageSearchType(type);
    }

    /**
     * Filters out field based strategies and returns all property keys that are being searched
     */
    public static List<String> getPropKeysFromStrategies(List<AqlUISearchStrategy> strategies) {
        return strategies.stream()
                .filter(strategy -> strategy instanceof AqlUIPropertySearchStrategy)
                .map(AqlUISearchStrategy::getSearchKey)
                .collect(Collectors.toList());
    }

    public static List<AqlUISearchStrategy> buildStrategiesFromSearchModel(List<AqlUISearchModel> searches) {
        return searches.stream()
                .map(search -> getStrategyByFieldId(search)
                        .comparator(search.getComparator())
                        .values(search.getValues()))
                .collect(Collectors.toList());
    }

    /**
     * Tries each of the criteria enums for the strategy of the given model
     */
    private static AqlUISearchStrategy getStrategyByFieldId(AqlUISearchModel search) {
        try {
            return PackageSearchCriteria.getStrategyByFieldId(search.getId());
        } catch (IllegalArgumentException iae) {
            return FieldSearchCriteria.getStrategyByFieldId(search.getId());
        }
    }

    public static PackageSearchCriteria getMatchingPackageSearchCriteria(FullRow row) {
        PackageSearchCriteria criterion;
        //Special case to differentiate between docker v1 and v2 image property which is the same
        if (row.getKey().equalsIgnoreCase("docker.repoName") && row.getPath()
                .contains("repositories/" + row.getValue())) {
            criterion = PackageSearchCriteria.dockerV1Image;
        } else {
            criterion = PackageSearchCriteria.getCriteriaByAqlFieldOrPropName(row.getKey());
        }
        return criterion;
    }
}