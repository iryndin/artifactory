/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.rest.resource.repositories;

import com.thoughtworks.xstream.XStream;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.constant.RepositoriesRestConstants;
import org.artifactory.api.xstream.XStreamFactory;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.util.List;

/**
 * A resource to manage the remote repository configuration actions
 *
 * @author Noam Tenne
 */
public class RepoConfigurationResource {

    private HttpServletResponse response;
    private RepositoryService repositoryService;
    private String remoteRepoKey;

    /**
     * Default constructor
     *
     * @param response          Response to write to
     * @param repositoryService Repository service
     * @param remoteRepoKey     Key of selected remote repository
     */
    public RepoConfigurationResource(HttpServletResponse response, RepositoryService repositoryService,
            String remoteRepoKey) {
        this.response = response;
        this.repositoryService = repositoryService;
        this.remoteRepoKey = remoteRepoKey;
    }

    /**
     * Returns the remote repository descriptor of the given key, if shared
     *
     * @return Json generated descriptor
     * @throws IOException
     */
    @GET
    @Produces(RepositoriesRestConstants.MT_REMOTE_REPOSITORY_CONFIGURATION)
    public RemoteRepoDescriptor getRemoteDescriptor() throws IOException {

        List<RemoteRepoDescriptor> remoteRepoDescriptors = repositoryService.getRemoteRepoDescriptors();
        for (RemoteRepoDescriptor remoteRepoDescriptor : remoteRepoDescriptors) {
            String currentRepoKey = remoteRepoDescriptor.getKey();
            if (remoteRepoKey.equals(currentRepoKey) && remoteRepoDescriptor.isShareConfiguration()) {
                return maskData(remoteRepoDescriptor);
            }
        }

        return null;
    }

    /**
     * Mask different elements of the given remote repository descriptor
     *
     * @param remoteRepoDescriptor Descriptor to mask
     * @return Masked copy of given descriptor
     */
    private RemoteRepoDescriptor maskData(RemoteRepoDescriptor remoteRepoDescriptor) {
        if (remoteRepoDescriptor instanceof HttpRepoDescriptor) {
            HttpRepoDescriptor httpRepoDescriptor = (HttpRepoDescriptor) remoteRepoDescriptor;
            XStream xStream = XStreamFactory.create(HttpRepoDescriptor.class);
            String descriptorXml = xStream.toXML(httpRepoDescriptor);
            HttpRepoDescriptor descriptorCopy = (HttpRepoDescriptor) xStream.fromXML(descriptorXml);

            //Remote credentials
            descriptorCopy.setUsername(null);
            descriptorCopy.setPassword(null);

            //Remove proxy config
            descriptorCopy.setProxy(null);

            //Remove local address
            descriptorCopy.setLocalAddress(null);

            return descriptorCopy;
        }
        return remoteRepoDescriptor;
    }
}