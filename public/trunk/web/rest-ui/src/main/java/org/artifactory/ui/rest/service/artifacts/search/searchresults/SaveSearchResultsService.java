package org.artifactory.ui.rest.service.artifacts.search.searchresults;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.search.ArtifactSearchAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.search.ItemSearchResult;
import org.artifactory.api.search.SavedSearchResults;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.security.ArtifactoryPermission;
import org.artifactory.ui.rest.model.artifacts.search.BaseSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SaveSearchResultsService implements RestService {

    @Autowired
    AuthorizationService authorizationService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        String searchName = request.getQueryParamByKey("name");
        List<ItemSearchResult> results = new ArrayList<>();
        List<BaseSearchResult> baseSearchResults = (List<BaseSearchResult>) request.getImodel();
        baseSearchResults.forEach(result->results.add(result.getSearchResult()));
        AddonsManager addonsManager  = ContextHelper.get().beanForType(AddonsManager.class);
        ArtifactSearchAddon artifactSearchAddon = addonsManager.addonByType(ArtifactSearchAddon.class);
        SavedSearchResults searchResults = artifactSearchAddon.getSearchResults(searchName, results, true);
        setResults(searchResults,request.getServletRequest());
    }

    public void setResults(SavedSearchResults savedSearchResults,HttpServletRequest request) {
        AuthorizationService authService = ContextHelper.get().getAuthorizationService();
        if (authService.hasPermission(ArtifactoryPermission.DEPLOY)) {
            request.getSession(false).setAttribute(savedSearchResults.getName(), savedSearchResults);
        }
    }
}
