/*
 * This file is part of Artifactory.
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
import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.jackson.JacksonFactory;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.xstream.XStreamFactory;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.codehaus.jackson.JsonGenerator;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.util.List;

/**
 * A resource to manage all remote repository actions
 *
 * @author Noam Tenne
 */
public class RemoteRepoResource {

    private HttpServletResponse response;
    private RepositoryService repositoryService;

    /**
     * Default constructor
     *
     * @param response          Response to write to
     * @param repositoryService Repository service
     */
    public RemoteRepoResource(HttpServletResponse response, RepositoryService repositoryService) {
        this.response = response;
        this.repositoryService = repositoryService;
    }

    /**
     * Returns all the shared remote repository descriptors
     *
     * @return Json generated descriptor map
     * @throws IOException
     */
    @GET
    @Produces("application/vnd.org.jfrog.artifactory.RemoteRepoDescriptorMap+json")
    public String getAllRemoteDescriptors() throws IOException {
        ServletOutputStream stream = response.getOutputStream();
        JsonGenerator jsonGenerator = JacksonFactory.create(stream);

        OrderedMap<String, RemoteRepoDescriptor> descriptorOrderedMap =
                new ListOrderedMap<String, RemoteRepoDescriptor>();
        List<RemoteRepoDescriptor> remoteRepoDescriptors = repositoryService.getRemoteRepoDescriptors();
        for (RemoteRepoDescriptor remoteRepoDescriptor : remoteRepoDescriptors) {
            if (remoteRepoDescriptor.isShareConfiguration()) {
                RemoteRepoDescriptor maskedDescriptor = maskCredentials(remoteRepoDescriptor);
                descriptorOrderedMap.put(remoteRepoDescriptor.getKey(), maskedDescriptor);
            }
        }
        if (descriptorOrderedMap.isEmpty()) {
            return "There are no shared remote repository configurations.";
        }

        jsonGenerator.writeObject(descriptorOrderedMap);
        return "";
    }

    /**
     * Returns the remote repository descriptor of the given key, if shared
     *
     * @param remoteRepoKey Key of remote repo
     * @return Json generated descriptor
     * @throws IOException
     */
    @GET
    @Path("{remoteRepoKey}")
    @Produces("application/vnd.org.jfrog.artifactory.RemoteRepoDescriptor+json")
    public String getRemoteDescriptor(@PathParam("remoteRepoKey") String remoteRepoKey) throws IOException {
        if (StringUtils.isEmpty(remoteRepoKey)) {
            response.setStatus(HttpStatus.SC_NOT_FOUND);
            return "Given repository name is either null or empty.";
        }

        ServletOutputStream stream = response.getOutputStream();
        JsonGenerator jsonGenerator = JacksonFactory.create(stream);

        List<RemoteRepoDescriptor> remoteRepoDescriptors = repositoryService.getRemoteRepoDescriptors();
        for (RemoteRepoDescriptor remoteRepoDescriptor : remoteRepoDescriptors) {
            String currentRepoKey = remoteRepoDescriptor.getKey();
            if (remoteRepoKey.equals(currentRepoKey) && remoteRepoDescriptor.isShareConfiguration()) {
                RemoteRepoDescriptor maskedDescriptor = maskCredentials(remoteRepoDescriptor);
                jsonGenerator.writeObject(maskedDescriptor);
            }
        }
        return "";
    }

    public RemoteRepoDescriptor maskCredentials(RemoteRepoDescriptor remoteRepoDescriptor) {
        if (remoteRepoDescriptor instanceof HttpRepoDescriptor) {
            HttpRepoDescriptor httpRepoDescriptor = (HttpRepoDescriptor) remoteRepoDescriptor;
            XStream xStream = XStreamFactory.create(HttpRepoDescriptor.class);
            String descriptorXml = xStream.toXML(httpRepoDescriptor);
            HttpRepoDescriptor descriptorCopy = (HttpRepoDescriptor) xStream.fromXML(descriptorXml);
            descriptorCopy.setUsername("");
            descriptorCopy.setPassword("");

            return descriptorCopy;
        }
        return remoteRepoDescriptor;
    }
}
