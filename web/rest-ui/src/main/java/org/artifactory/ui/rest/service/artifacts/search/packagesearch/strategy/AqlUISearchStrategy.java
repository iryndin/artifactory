package org.artifactory.ui.rest.service.artifacts.search.packagesearch.strategy;

import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlFieldEnum;

import java.util.List;

/**
 * @author Dan Feldman
 */
public interface AqlUISearchStrategy {

    /**
     * Sets the values to search for in the chosen field
     */
    AqlUISearchStrategy values(List<String> values);

    /**
     * Sets the values to search for in the chosen field
     */
    AqlUISearchStrategy values(String... values);

    /**
     * Sets the comparator to search with in the chosen field
     */
    AqlUISearchStrategy comparator(AqlComparatorEnum comparator);

    /**
     * Returns the field being searched
     */
    AqlFieldEnum getSearchField();

    /**
     * Returns the property key being searched for all property based strategies.
     */
    String getSearchKey();

    /**
     * Constructs a ready-to-go or clause based on the chosen strategy and requested criteria.
     */
    AqlBase.OrClause toQuery();

    boolean includePropsInResult();
}
