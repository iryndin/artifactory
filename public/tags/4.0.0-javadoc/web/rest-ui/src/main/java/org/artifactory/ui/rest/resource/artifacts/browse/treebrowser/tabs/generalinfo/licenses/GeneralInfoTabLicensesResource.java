package org.artifactory.ui.rest.resource.artifacts.browse.treebrowser.tabs.generalinfo.licenses;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.general.licenses.GeneralTabLicenseModel;
import org.artifactory.ui.rest.resource.BaseResource;
import org.artifactory.ui.rest.service.artifacts.browse.BrowseServiceFactory;
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
 * @author Dan Feldman
 */
@Path("generalTabLicenses")
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GeneralInfoTabLicensesResource extends BaseResource {

    @Autowired
    BrowseServiceFactory browseFactory;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllAvailableLicenses() throws Exception {
        return runService(browseFactory.getAllAvailableLicenses());
    }

    @GET
    @Path("getArchiveLicenseFile")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getArchiveLicenseFile() throws Exception {
        return runService(browseFactory.getArchiveLicenseFile());
    }

    @PUT
    @Path("setLicensesOnPath")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setLicensesOnPath(List<GeneralTabLicenseModel> licenses) throws Exception {
        return runService(browseFactory.setLicensesOnPath(), licenses);
    }

    @GET
    @Path("scanArtifact")
    @Produces(MediaType.APPLICATION_JSON)
    public Response scanArtifactForLicenses() throws Exception {
        return runService(browseFactory.scanArtifactForLicenses());
    }

    @POST
    @Path("queryCodeCenter")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryCodeCenter() throws Exception {
        return runService(browseFactory.queryCodeCenter());
    }
}
