package org.artifactory.ui.rest.model.artifacts.search.packagesearch.result;

import com.google.common.collect.HashMultimap;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.result.rows.FullRow;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.Collection;
import java.util.Map;

/**
 * Interface for all 'Package Search' results (that can actually be any result) - basic fields are members based on
 * the domain, any extra included fields (i.e. AqlApiItem.create().filter(...).include()) are inserted in the map.
 *
 * @author Dan Feldman
 */
public interface AqlUISearchResult {

    @JsonIgnore
    AqlDomainEnum getDomain();

    //Returns a Jackson serializable map representing the extra fields multimap.
    Map<String, Collection<String>> getExtraFields();

    //Used by AqlUISearchResultManipulator to change or add values to the extra fields multimap.
    HashMultimap<String, String> getExtraFieldsMap();

    AqlUISearchResult aggregateRow(FullRow row);

}
