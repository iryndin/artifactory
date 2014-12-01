/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.search.property;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.property.PropertySearchControls;
import org.artifactory.api.search.property.PropertySearchResult;
import org.artifactory.aql.AqlConverts;
import org.artifactory.aql.AqlService;
import org.artifactory.aql.api.AqlApi;
import org.artifactory.aql.api.AqlArtifactApi;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.result.AqlQueryResultIfc;
import org.artifactory.aql.result.rows.AqlArtifact;
import org.artifactory.search.SearcherBase;
import org.artifactory.storage.spring.StorageContextHelper;

import java.util.List;
import java.util.Set;

import static org.artifactory.aql.api.AqlApi.*;
import static org.artifactory.aql.model.AqlComparatorEnum.equals;

/**
 * @author Gidi Shabat
 * @author Yossi Shaul
 */
public class PropertySearcherAql extends SearcherBase<PropertySearchControls, PropertySearchResult> {

    @Override
    public ItemSearchResults<PropertySearchResult> doSearch(PropertySearchControls controls) {
        Multimap<String, String> properties = controls.getProperties();

        AqlApi.AndClause and = and();
        // The artifact should exists in one of the repositories therefore the relation between the repos is OR
        List<String> selectedRepoForSearch = controls.getSelectedRepoForSearch();
        AqlApi.OrClause or = or();
        if (selectedRepoForSearch != null) {
            for (String repoKey : selectedRepoForSearch) {
                or.append(AqlApi.artifactRepo(equals, repoKey));
            }
        }
        and.append(or);
        Set<String> openness = controls.getPropertyKeysByOpenness(true);
        for (String key : openness) {
            for (String value : properties.get(key)) {
                if (value == null || "*".equals(value.trim())) {
                    and.append(propertyKey(AqlComparatorEnum.matches, key));
                } else {
                    and.append(AqlApi.freezeJoin(and(propertyKey(AqlComparatorEnum.matches, key),
                            propertyValue(AqlComparatorEnum.matches, value))));
                }
            }
        }
        Set<String> closeness = controls.getPropertyKeysByOpenness(false);
        for (String key : closeness) {
            for (String value : properties.get(key)) {
                if (value == null) {
                    and.append(propertyKey(AqlComparatorEnum.equals, key));
                } else {
                    and.append(property(key, AqlComparatorEnum.equals, value));
                }
            }
        }
        AqlArtifactApi aqlQuery = AqlApi.findArtifacts().filter(and).limit(getLimit(controls));
        AqlService aqlService = StorageContextHelper.get().beanForType(AqlService.class);
        AqlQueryResultIfc<AqlArtifact> result = aqlService.executeQueryEager(aqlQuery);
        //Return global results list
        long totalResultCount = result.getSize();
        Set<PropertySearchResult> globalResults = Sets.newLinkedHashSet();
        for (AqlArtifact aqlArtifact : result.getResults()) {
            globalResults.add(new PropertySearchResult(AqlConverts.toFileInfo.apply(aqlArtifact)));
        }

        return new ItemSearchResults<>(Lists.newArrayList(globalResults), totalResultCount);
    }
}