package org.artifactory.ui.rest.service.artifacts.search;

import org.artifactory.ui.rest.model.artifacts.search.DeleteArtifactsModel;
import org.artifactory.ui.rest.service.artifacts.search.checksumsearch.ChecksumSearchService;
import org.artifactory.ui.rest.service.artifacts.search.classsearch.ClassSearchService;
import org.artifactory.ui.rest.service.artifacts.search.gavcsearch.GavcSearchService;
import org.artifactory.ui.rest.service.artifacts.search.packagesearch.GetPackageSearchOptionsService;
import org.artifactory.ui.rest.service.artifacts.search.packagesearch.PackageSearchCriteriaToNativeAqlService;
import org.artifactory.ui.rest.service.artifacts.search.packagesearch.PackageSearchService;
import org.artifactory.ui.rest.service.artifacts.search.propertysearch.GetPropertySetsService;
import org.artifactory.ui.rest.service.artifacts.search.propertysearch.PropertySearchService;
import org.artifactory.ui.rest.service.artifacts.search.quicksearch.QuickSearchService;
import org.artifactory.ui.rest.service.artifacts.search.remotesearch.RemoteSearchService;
import org.artifactory.ui.rest.service.artifacts.search.searchresults.*;
import org.artifactory.ui.rest.service.artifacts.search.trashsearch.TrashSearchService;
import org.springframework.beans.factory.annotation.Lookup;

/**
 * @author Chen Keinan
 */
public abstract class SearchServiceFactory {

    // search services
    @Lookup
    public abstract QuickSearchService quickSearchService();

    @Lookup
    public abstract ClassSearchService classSearchService();

    @Lookup
    public abstract GavcSearchService gavcSearchService();

    @Lookup
    public abstract PropertySearchService propertySearchService();

    @Lookup
    public abstract PackageSearchService packageSearch();

    @Lookup
    public abstract PackageSearchCriteriaToNativeAqlService PackageSearchCriteriaToNativeAql();

    @Lookup
    public abstract GetPackageSearchOptionsService packageSearchOptions();

    @Lookup
    public abstract SaveSearchResultsService saveSearchResults();

    @Lookup
    public abstract GetPropertySetsService getPropertySetsService();

    @Lookup
    public abstract ChecksumSearchService checksumSearchService();

    @Lookup
    public abstract RemoteSearchService remoteSearchService();

    @Lookup
    public abstract TrashSearchService trashSearchService();

    @Lookup
    public abstract DeleteArtifactsService<DeleteArtifactsModel> deleteArtifactsService();

    @Lookup
    public abstract GetSearchResultsService getSearchResults();

    @Lookup
    public abstract RemoveSearchResultsService removeSearchResults();

    @Lookup
    public abstract SubtractSearchResultsService subtractSearchResults();

    @Lookup
    public abstract IntersectSearchResultsService intersectSearchResults();

    @Lookup
    public abstract AddSearchResultsService addSearchResults();

    @Lookup
    public abstract ExportSearchResultsService exportSearchResults();

    @Lookup
    public abstract CopySearchResultsService copySearchResults();

    @Lookup
    public abstract MoveSearchResultsService moveSearchResults();

    @Lookup
    public abstract DiscardFromResultsService discardResults();
}
