package org.artifactory.ui.rest.service.artifacts.search.packagesearch;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;
import org.apache.http.HttpStatus;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.aql.AqlService;
import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.aql.result.rows.AqlBaseFullRowImpl;
import org.artifactory.aql.util.AqlUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.search.SearchResult;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.criteria.PackageSearchCriteria;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.result.PackageSearchResult;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.search.AqlUISearchModel;
import org.artifactory.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.artifactory.ui.rest.service.artifacts.search.packagesearch.util.PackageSearchHelper.buildItemQuery;

/**
 * Search service that provides an abstraction over aql for UI searches.
 * Using {@link PackageSearchCriteria} you can specify any combination of field / property criterion from any aql domain
 * and link it to proper search and result models for the UI to consume
 *
 * @author Dan Feldman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PackageSearchService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(PackageSearchService.class);

    @Autowired
    AqlService aqlService;

    @Autowired
    AuthorizationService authService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        List<AqlUISearchModel> searches = (List<AqlUISearchModel>) request.getModels();
        if (CollectionUtils.isNullOrEmpty(searches)) {
            log.debug("Got empty search criteria for Package Search.");
            response.error("Search criteria cannot be empty.");
            response.responseCode(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        SearchResult model = search(searches);
        setDownloadLinksOnModel((Set<PackageSearchResult>) model.getResults(), request);
        model.addNotifications(response);
        response.iModel(model);
    }

    public SearchResult search(List<AqlUISearchModel> searches) {
        //Build the query here to return native in response
        AqlBase query = buildItemQuery(searches, true /*, boolean includeExtraFieldsInResult*/);
        String nativeQuery = query.toNative(0);
        log.debug("strategies resolved to query: " + nativeQuery);
        Set<PackageSearchResult> results = executeSearch(query);

        // update response data
        return new SearchResult(results, nativeQuery, results.size(), true);
    }

    private Set<PackageSearchResult> executeSearch(/*AqlDomainEnum domain,*/ AqlBase query) {
        Set<PackageSearchResult> results = Sets.newHashSet();
        long timer = System.currentTimeMillis();
        List<AqlBaseFullRowImpl> queryResults = aqlService.executeQueryEager(query).getResults();
        HashMultimap<RepoPath, AqlBaseFullRowImpl> resultsByPath = aggregateResultsByPath(queryResults);
        reduceAggregatedResults(results, resultsByPath);
        manipulateResultFields(results);
        log.trace("Search found {} results in {} milliseconds", results.size(), System.currentTimeMillis() - timer);
        return results;
    }

    /**
     * Reduces all rows of a single path into one result that represents a single artifact and all extra fields that
     * were requested with it and are a part of its criteria domain.
     */
    private void reduceAggregatedResults(Set<PackageSearchResult> results,
            HashMultimap<RepoPath, AqlBaseFullRowImpl> resultsByPath) {
        Set<PackageSearchResult> concurentSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
        resultsByPath.keySet().parallelStream()
                .forEach(path -> concurentSet.add(
                        resultsByPath.get(path).stream()
                                .reduce(new PackageSearchResult(resultsByPath.get(path).iterator().next()),
                                        PackageSearchResult::aggregateRow,
                                        PackageSearchResult::merge)
                ));
        results.addAll(concurentSet);
    }

    private void setDownloadLinksOnModel(Set<PackageSearchResult> results, ArtifactoryRestRequest request) {
        results.stream()
                .forEach(result -> result.setDownloadLinkAndActions(request));
    }

    /**
     * Maps repo path to all rows returned by the search for it. Also filters out results based on user permissions.
     */
    private HashMultimap<RepoPath, AqlBaseFullRowImpl> aggregateResultsByPath(List<AqlBaseFullRowImpl> results) {
        HashMultimap<RepoPath, AqlBaseFullRowImpl> aggregator = HashMultimap.create();
        for (AqlBaseFullRowImpl result : results) {
            RepoPath path = AqlUtils.fromAql(result);
            if (authService.canRead(path)) {
                aggregator.put(path, result);
            } else {
                log.debug("Path '{}' omitted from results due to missing read permissions for user {}", path.toPath(),
                        authService.currentUsername());
            }
        }
        return aggregator;
    }

    /**
     * Calls all AqlUISearchResultManipulators associated with each result's PackageType to manipulate
     * any relevant fields that should be changed before returning to the UI.
     */
    private void manipulateResultFields(Set<PackageSearchResult> results) {
        results.stream()
                .forEach(result -> PackageSearchCriteria.getResultManipulatorsByPackage(result.getPackageType())
                                .stream()
                                .forEach(manipulator -> manipulator.manipulate(result))
                );
    }

    // TODO: [by dan] for advanced (text-box) search in UI
    //private AqlBase buildQuery(String query) {
    // List<AqlBaseFullRowImpl> queryResults = aqlService.executeQueryEager(query).getResults();
    //    return null;
    //}

    ///**
    // * Goes over the given strategies and finds if any extra fields from domains other then 'item' are requested
    // * for this query - if yes add the relevant includes to the query.
    // */
    // TODO: [by dan] implement if we decide to allow adding fields from different domains into the package search
    //private void includeExtraFieldsInResultIfNeeded(List<AqlUISearchStrategy> strategies, AqlApiItem query,
    //        boolean includeExtraFieldsInResult) { }
}
