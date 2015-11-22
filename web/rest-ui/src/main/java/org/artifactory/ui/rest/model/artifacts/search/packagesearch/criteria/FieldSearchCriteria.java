package org.artifactory.ui.rest.model.artifacts.search.packagesearch.criteria;

import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.search.AqlUISearchModel;
import org.artifactory.ui.rest.service.artifacts.search.packagesearch.strategy.AqlUIFieldSearchStrategy;
import org.artifactory.ui.rest.service.artifacts.search.packagesearch.strategy.AqlUISearchStrategy;

/**
 * Contains all available field criteria that is globally supported for every package
 *
 * @author Dan Feldman
 */
public enum FieldSearchCriteria {

    repo(new AqlUISearchModel("repo", "Repository", "Repository", new AqlComparatorEnum[]{AqlComparatorEnum.equals}),
            new AqlUIFieldSearchStrategy(AqlFieldEnum.itemRepo, new AqlDomainEnum[]{AqlDomainEnum.items}));

    AqlUISearchModel model;
    AqlUISearchStrategy strategy;

    FieldSearchCriteria(AqlUISearchModel model, AqlUISearchStrategy strategy) {
        this.model = model;
        this.strategy = strategy;
    }


    public AqlUISearchModel getModel() {
        return model;
    }

    public AqlUISearchStrategy getStrategy() {
        return strategy;
    }

    public static AqlUISearchStrategy getStrategyByFieldId(String id) {
        return valueOf(id).strategy;
    }

    /**
     * Returns criteria that matches the AQL field name or the property key that {@param aqlName} references
     *//*
    public static FieldSearchCriteria getCriteriaByAqlFieldOrPropName(String aqlName) {
        return Stream.of(values())
                .filter(value -> value.aqlName.equalsIgnoreCase(aqlName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported field or property '" + aqlName + "'."));
    }
    */
}