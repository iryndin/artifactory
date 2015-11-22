package org.artifactory.ui.rest.service.artifacts.search.packagesearch;

import org.apache.http.HttpStatus;
import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.criteria.PackageSearchCriteria;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.search.AqlUISearchModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

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
public class PackageSearchCriteriaToNativeAqlService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(PackageSearchCriteriaToNativeAqlService.class);

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        List<AqlUISearchModel> searches = (List<AqlUISearchModel>) request.getModels();
        //Build the query here to return native in response
        AqlBase query = buildItemQuery(searches, true /*, boolean includeExtraFieldsInResult*/);
        response.iModel(query.toNative(0)).responseCode(HttpStatus.SC_OK);
    }
}