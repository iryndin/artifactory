package org.artifactory.ui.rest.resource.artifacts.browse.treebrowser.actions;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.model.artifact.BaseArtifact;
import org.artifactory.rest.common.resource.BaseResource;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.action.*;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.action.recalculateindex.BaseIndexCalculator;
import org.artifactory.ui.rest.service.artifacts.browse.BrowseServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Chen Keinan
 */
@Path("artifactactions")
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ArtifactActionsResource extends BaseResource {

    @Autowired
    BrowseServiceFactory browseFactory;

    @Autowired
    @Qualifier("streamingRestResponse")
    public void setArtifactoryResponse(RestResponse artifactoryResponse) {
        this.artifactoryResponse = artifactoryResponse;
    }

    @POST
    @Path("copy")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response copyArtifact(CopyArtifact copyArtifact)
            throws Exception {
        return runService(browseFactory.copyArtifactService(), copyArtifact);
    }

    @POST
    @Path("move")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response moveArtifact(MoveArtifact copyAction)
            throws Exception {
        return runService(browseFactory.moveArtifactService(), copyAction);
    }

    @POST
    @Path("download")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response downloadArtifact(DownloadArtifact downloadArtifact)
            throws Exception {
        return runService(browseFactory.downloadArtifactService(), downloadArtifact);
    }

    @GET
    @Path("downloadfolderinfo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFolderDownloadInfo() throws Exception {
        return runService(browseFactory.getDownloadFolderInfo());
    }

    @POST
    @Path("emptytrash")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response emptyTrash() throws Exception {
        return runService(browseFactory.emptyTrashService());
    }

    @POST
    @Path("restore")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response restoreFromTrash(RestoreArtifact artifact) throws Exception {
        return runService(browseFactory.restoreArtifactService(), artifact);
    }

    @GET
    @Path("downloadfolder")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFolder() throws Exception {
        return runService(browseFactory.downloadFolder());
    }

    @POST
    @Path("watch")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response watchArtifact(WatchArtifact watchArtifact)
            throws Exception {
        return runService(browseFactory.watchArtifactService(), watchArtifact);
    }

    @POST
    @Path("delete")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteArtifact(DeleteArtifact deleteArtifact)
            throws Exception {
        return runService(browseFactory.deleteArtifactService(), deleteArtifact);
    }

    @POST
    @Path("view")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response viewArtifact(ViewArtifact viewArtifact)
            throws Exception {
        return runService(browseFactory.viewArtifactService(), viewArtifact);
    }

    @POST
    @Path("zap")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response zapArtifact(ZapArtifact zapArtifact)
            throws Exception {
        return runService(browseFactory.zapArtifactService(), zapArtifact);
    }

    @POST
    @Path("calculateIndex")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response calculateIndex(BaseIndexCalculator baseIndexCalculator)
            throws Exception {
        return runService(browseFactory.recalculateIndex(), baseIndexCalculator);
    }

    @POST
    @Path("zapVirtual")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response zapVirtual(ZapArtifact zapArtifact)
            throws Exception {
        return runService(browseFactory.zapCachesVirtual(), zapArtifact);
    }

    @POST
    @Path("addSha256")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addSha256(BaseArtifact baseArtifact)
            throws Exception {
        return runService(browseFactory.addSha256ToArtifact(), baseArtifact);
    }


}
