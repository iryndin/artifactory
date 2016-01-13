package org.artifactory.ui.rest.service.artifacts.search.trashsearch;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.SearchControls;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.search.artifact.ArtifactSearchControls;
import org.artifactory.api.search.artifact.ArtifactSearchResult;
import org.artifactory.api.search.artifact.ChecksumSearchControls;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.ConstantValues;
import org.artifactory.repo.trash.TrashService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.search.SearchResult;
import org.artifactory.ui.rest.model.artifacts.search.trashsearch.TrashSearch;
import org.artifactory.ui.rest.model.artifacts.search.trashsearch.TrashSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class TrashSearchService implements RestService {

    @Autowired
    private SearchService searchService;

    @Autowired
    private RepositoryService repoService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        TrashSearch trashSearch = (TrashSearch) request.getImodel();
        if (trashSearch.getIsChecksum()) {
            performChecksumSearch(trashSearch, response);
        } else {
            performQuickSearch(trashSearch, response);
        }
    }

    private void performChecksumSearch(TrashSearch trashSearch, RestResponse response) {
        ChecksumSearchControls checksumSearchControl = getChecksumSearchControl(trashSearch);
        if (isSearchEmptyOrWildCardOnly(checksumSearchControl)) {
            response.error("Please enter a valid checksum to search for");
            return;
        }

        ItemSearchResults<ArtifactSearchResult> checksumResults =
                searchService.getArtifactsByChecksumResults(checksumSearchControl);
        updateResponse(trashSearch, checksumSearchControl.isLimitSearchResults(), checksumResults, response);
    }

    private void performQuickSearch(TrashSearch trashSearch, RestResponse response) {
        ArtifactSearchControls artifactSearchControl = getArtifactSearchControl(trashSearch);
        if (isSearchEmptyOrWildCardOnly(artifactSearchControl)) {
            response.error("Search term empty or containing only wildcards is not permitted");
            return;
        }

        ItemSearchResults<ArtifactSearchResult> checksumResults =
                searchService.searchArtifacts(artifactSearchControl);
        updateResponse(trashSearch, artifactSearchControl.isLimitSearchResults(), checksumResults, response);
    }

    private void updateResponse(TrashSearch trashSearch, boolean limit,
                                ItemSearchResults<ArtifactSearchResult> searchResults, RestResponse response) {
        List<TrashSearchResult> trashSearchResults = Lists.newArrayList();
        for (ArtifactSearchResult artifactSearchResult : searchResults.getResults()) {
            trashSearchResults.add(new TrashSearchResult(artifactSearchResult,
                    repoService.getProperties(artifactSearchResult.getItemInfo().getRepoPath())));
        }
        long resultsCount;
        int maxResults = ConstantValues.searchMaxResults.getInt();
        if (limit && trashSearchResults.size() > maxResults) {
            trashSearchResults = trashSearchResults.subList(0, maxResults);
            resultsCount = trashSearchResults.size() == 0 ? 0 : searchResults.getFullResultsCount();
        } else {
            resultsCount = trashSearchResults.size();
        }
        SearchResult model = new SearchResult(trashSearchResults, trashSearch.getQuery(), resultsCount, limit);
        model.addNotifications(response);
        response.iModel(model);
    }

    private ChecksumSearchControls getChecksumSearchControl(TrashSearch trashSearch) {
        String query = trashSearch.getQuery();
        ChecksumSearchControls searchControls = new ChecksumSearchControls();
        if (StringUtils.isNotBlank(query)) {
            if (StringUtils.length(query) == ChecksumType.md5.length()) {
                searchControls.addChecksum(ChecksumType.md5, query);
                searchControls.setLimitSearchResults(true);
            } else if (StringUtils.length(query) == ChecksumType.sha1.length()) {
                searchControls.addChecksum(ChecksumType.sha1, query);
            }
            searchControls.setSelectedRepoForSearch(Lists.newArrayList(TrashService.TRASH_KEY));
        }
        return searchControls;
    }

    private ArtifactSearchControls getArtifactSearchControl(TrashSearch trashSearch) {
        ArtifactSearchControls artifactSearchControls = new ArtifactSearchControls();
        artifactSearchControls.setSelectedRepoForSearch(Lists.newArrayList(TrashService.TRASH_KEY));
        artifactSearchControls.setLimitSearchResults(true);
        artifactSearchControls.setQuery(trashSearch.getQuery());
        artifactSearchControls.setLimitSearchResults(true);
        return artifactSearchControls;
    }

    private boolean isSearchEmptyOrWildCardOnly(SearchControls artifactSearchControl) {
        return artifactSearchControl.isEmpty() || artifactSearchControl.isWildcardsOnly();
    }
}
