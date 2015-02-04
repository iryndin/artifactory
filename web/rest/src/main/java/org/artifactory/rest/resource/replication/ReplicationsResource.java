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

package org.artifactory.rest.resource.replication;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.rest.MissingRestAddonException;
import org.artifactory.addon.rest.RestAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.rest.constant.ReplicationsRestConstants;
import org.artifactory.api.rest.replication.ReplicationConfigRequest;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.replication.LocalReplicationDescriptor;
import org.artifactory.descriptor.replication.RemoteReplicationDescriptor;
import org.artifactory.descriptor.replication.ReplicationBaseDescriptor;
import org.artifactory.repo.Repo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.rest.common.exception.BadRequestException;
import org.artifactory.rest.common.exception.NotFoundException;
import org.artifactory.rest.util.RestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * REST resource for configuring local and remote replication.
 *
 * @author mamo
 */
@Component
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Path(ReplicationsRestConstants.ROOT)
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
public class ReplicationsResource {

    @Autowired
    private CentralConfigService centralConfigService;

    @Autowired
    private AddonsManager addonsManager;

    /**
     * Returns the replication for the given {@code repoKey}, if found
     */
    @GET
    @Path("{repoKey: .+}")
    @Produces({ReplicationsRestConstants.MT_REPLICATION_CONFIG_REQUEST, MediaType.APPLICATION_JSON})
    public Response get(@PathParam("repoKey") String repoKey) {
        verifyArtifactoryPro();
        verifyRepositoryExists(repoKey);

        CentralConfigDescriptor descriptor = centralConfigService.getDescriptor();
        switch (RestUtils.repoType(repoKey)) {

            case LOCAL:
                LocalReplicationDescriptor localReplication = descriptor.getLocalReplication(repoKey);
                if (localReplication == null) {
                    throw new NotFoundException("Could not find replication");
                }
                return Response.ok(localReplication).build();

            case REMOTE:
                RemoteReplicationDescriptor remoteReplication = descriptor.getRemoteReplication(repoKey);
                if (remoteReplication == null) {
                    throw new NotFoundException("Could not find replication");
                }
                return Response.ok(remoteReplication).build();

            default:
                throw new BadRequestException("Invalid repository");
        }
    }

    /**
     * Add or replace replication for given {@code repoKey}
     */
    @PUT
    @Consumes({ReplicationsRestConstants.MT_REPLICATION_CONFIG_REQUEST, MediaType.APPLICATION_JSON})
    @Path("{repoKey: .+}")
    public Response addOrReplace(@PathParam("repoKey") String repoKey, ReplicationConfigRequest replicationRequest) {
        verifyArtifactoryPro();
        verifyRepositoryExists(repoKey);

        CentralConfigDescriptor descriptor = centralConfigService.getMutableDescriptor();
        switch (RestUtils.repoType(repoKey)) {

            case LOCAL:
                LocalReplicationDescriptor localReplication = new LocalReplicationDescriptor();
                localReplication.setRepoKey(repoKey);
                ReplicationConfigRequestHelper.fillLocalReplicationDescriptor(replicationRequest, localReplication);
                ReplicationConfigRequestHelper.verifyLocalReplicationRequest(localReplication);
                addOrReplace(localReplication, descriptor.getLocalReplications());
                centralConfigService.saveEditedDescriptorAndReload(descriptor);
                return Response.status(Response.Status.CREATED).build();

            case REMOTE:
                RemoteReplicationDescriptor remoteReplication = new RemoteReplicationDescriptor();
                remoteReplication.setRepoKey(repoKey);
                ReplicationConfigRequestHelper.fillBaseReplicationDescriptor(replicationRequest, remoteReplication);
                ReplicationConfigRequestHelper.verifyBaseReplicationRequest(remoteReplication);
                addOrReplace(remoteReplication, descriptor.getRemoteReplications());
                centralConfigService.saveEditedDescriptorAndReload(descriptor);
                return Response.status(Response.Status.CREATED).build();

            default:
                throw new BadRequestException("Invalid repository");
        }
    }

    /**
     * Update existing replication for given {@code repoKey}
     */
    @POST
    //@PATCH
    @Consumes({ReplicationsRestConstants.MT_REPLICATION_CONFIG_REQUEST, MediaType.APPLICATION_JSON})
    @Path("{repoKey: .+}")
    public Response update(@PathParam("repoKey") String repoKey, ReplicationConfigRequest replicationRequest) {
        verifyArtifactoryPro();
        verifyRepositoryExists(repoKey);

        CentralConfigDescriptor descriptor = centralConfigService.getMutableDescriptor();
        switch (RestUtils.repoType(repoKey)) {

            case LOCAL:
                LocalReplicationDescriptor localReplication = descriptor.getLocalReplication(repoKey);
                if (localReplication == null) {
                    throw new BadRequestException("Could not find existing replication for update");
                }
                ReplicationConfigRequestHelper.fillLocalReplicationDescriptor(replicationRequest, localReplication);
                ReplicationConfigRequestHelper.verifyLocalReplicationRequest(localReplication);
                centralConfigService.saveEditedDescriptorAndReload(descriptor);
                return Response.ok().build();

            case REMOTE:
                RemoteReplicationDescriptor remoteReplication = descriptor.getRemoteReplication(repoKey);
                if (remoteReplication == null) {
                    throw new BadRequestException("Could not find existing replication for update");
                }
                ReplicationConfigRequestHelper.fillBaseReplicationDescriptor(replicationRequest, remoteReplication);
                ReplicationConfigRequestHelper.verifyBaseReplicationRequest(remoteReplication);
                centralConfigService.saveEditedDescriptorAndReload(descriptor);
                return Response.ok().build();

            default:
                throw new BadRequestException("Invalid repository");
        }
    }

    /**
     * Delete existing replication for given {@code repoKey}
     */
    @DELETE
    @Path("{repoKey: .+}")
    public Response delete(@PathParam("repoKey") String repoKey) {
        verifyArtifactoryPro();
        verifyRepositoryExists(repoKey);

        CentralConfigDescriptor descriptor = centralConfigService.getMutableDescriptor();
        switch (RestUtils.repoType(repoKey)) {

            case LOCAL:
                LocalReplicationDescriptor localReplication = descriptor.getLocalReplication(repoKey);
                if (localReplication == null) {
                    throw new BadRequestException("Could not find existing replication for update");
                }
                descriptor.getLocalReplications().remove(localReplication);
                centralConfigService.saveEditedDescriptorAndReload(descriptor);
                return Response.ok().build();

            case REMOTE:
                RemoteReplicationDescriptor remoteReplication = descriptor.getRemoteReplication(repoKey);
                if (remoteReplication == null) {
                    throw new BadRequestException("Could not find existing replication for update");
                }
                descriptor.getRemoteReplications().remove(remoteReplication);
                centralConfigService.saveEditedDescriptorAndReload(descriptor);
                return Response.ok().build();

            default:
                throw new BadRequestException("Invalid repository");
        }
    }

    private void verifyArtifactoryPro() {
        if (addonsManager.addonByType(RestAddon.class).isDefault()) {
            throw new MissingRestAddonException();
        }
    }

    private Repo verifyRepositoryExists(String repoKey) {
        Repo repo = repositoryByKey(repoKey);
        if (repo == null) {
            throw new BadRequestException("Could not find repository");
        }
        return repo;
    }

    private Repo repositoryByKey(String repoKey) {
        return ((InternalRepositoryService) ContextHelper.get().getRepositoryService()).repositoryByKey(repoKey);
    }

    private <T extends ReplicationBaseDescriptor> void addOrReplace(T newReplication, List<T> replications) {
        String repoKey = newReplication.getRepoKey();
        T existingReplication = getReplication(repoKey, replications);
        if (existingReplication != null) {
            int i = replications.indexOf(existingReplication);
            replications.set(i, newReplication); //replace
        } else {
            replications.add(newReplication); //add
        }
    }

    private <T extends ReplicationBaseDescriptor> T getReplication(String replicatedRepoKey, List<T> replications) {
        for (T replication : replications) {
            if (replicatedRepoKey.equals(replication.getRepoKey())) {
                return replication;
            }
        }
        return null;
    }
}
