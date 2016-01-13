package org.artifactory.ui.rest.resource.artifacts.search;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.ui.rest.model.artifacts.search.DeleteArtifactsModel;
import org.artifactory.ui.rest.model.artifacts.search.checksumsearch.ChecksumSearch;
import org.artifactory.ui.rest.model.artifacts.search.classsearch.ClassSearch;
import org.artifactory.ui.rest.model.artifacts.search.gavcsearch.GavcSearch;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.search.AqlUISearchModel;
import org.artifactory.ui.rest.model.artifacts.search.propertysearch.PropertySearch;
import org.artifactory.ui.rest.model.artifacts.search.quicksearch.QuickSearch;
import org.artifactory.ui.rest.model.artifacts.search.remotesearch.RemoteSearch;
import org.artifactory.ui.rest.model.artifacts.search.trashsearch.TrashSearch;
import org.artifactory.ui.rest.service.artifacts.search.SearchServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Chen Keinan
 */
@Path("artifactsearch")
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ArtifactSearchResource extends BaseResource {

    @Autowired
    SearchServiceFactory searchFactory;

    @POST
    @Path("quick")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response quickSearch(QuickSearch quickSearch)
            throws Exception {
        return runService(searchFactory.quickSearchService(), quickSearch);
    }

    @POST
    @Path("class")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response classSearch(ClassSearch classSearch)
            throws Exception {
        return runService(searchFactory.classSearchService(), classSearch);
    }

    @POST
    @Path("gavc")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response gavcSearch(GavcSearch gavcSearch)
            throws Exception {
        return runService(searchFactory.gavcSearchService(), gavcSearch);
    }

    @POST
    @Path("pkg{type:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response pkgSearch(List<AqlUISearchModel> search) throws Exception {
        return runService(searchFactory.packageSearch(), search);
    }

    @GET
    @Path("pkg{type:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPkgSearchOptions() throws Exception {
        return runService(searchFactory.packageSearchOptions());
    }

    @POST
    @Path("pkg/tonative")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchCriteriaToNativeAql(List<AqlUISearchModel> search) throws Exception {
        return runService(searchFactory.PackageSearchCriteriaToNativeAql(), search);
    }

    @POST
    @Path("checksum")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checksumSearch(ChecksumSearch checksumSearch)
            throws Exception {
        if (matchToSha256Length(checksumSearch)){
            PropertySearch sha256Property = getPropertySearchModel("sha256", checksumSearch.getChecksum());
            return runService(searchFactory.propertySearchService(), sha256Property);
        }
        return runService(searchFactory.checksumSearchService(), checksumSearch);
    }


    @POST
    @Path("remote")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remoteSearch(RemoteSearch remoteSearch)
            throws Exception {
        return runService(searchFactory.remoteSearchService(), remoteSearch);
    }

    @POST
    @Path("trash")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response trashSearch(TrashSearch trashSearch)
            throws Exception {
        return runService(searchFactory.trashSearchService(), trashSearch);
    }

    @POST
    @Path("deleteArtifact")
    public Response deleteArtifacts(DeleteArtifactsModel deleteArtifactsModel)
            throws Exception {
        return runService(searchFactory.deleteArtifactsService(), deleteArtifactsModel);
    }

    /**
     * check if checksum length match sha 256 length
     * @param checksumSearch - checksum search object
     * @return - true if match sha256
     */
    private boolean matchToSha256Length(ChecksumSearch checksumSearch) {
        return StringUtils.length(checksumSearch.getChecksum()) == ChecksumType.sha256.length();
    }

    /**
     * create and return property search model
     * @param key - property key
     * @param value property value
     * @return property search model
     */
    private PropertySearch getPropertySearchModel(String key,String value){
        PropertySearch propertySearch = new PropertySearch();
        propertySearch.updatePropertySearchData(key, value);
        return propertySearch;
    }

}
