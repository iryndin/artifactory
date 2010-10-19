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

package org.artifactory.addon.plugin.services;

import org.artifactory.api.properties.PropertiesService;
import org.artifactory.common.StatusHolder;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.property.Property;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.ItemInfo;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.MetadataInfo;
import org.artifactory.md.Properties;
import org.artifactory.repo.*;
import org.artifactory.repo.service.InternalRepositoryService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

@Component
public class RepositoriesImpl implements Repositories {

    private static final Logger log = LoggerFactory.getLogger(RepositoriesImpl.class);

    @Inject
    private InternalRepositoryService repositoryService;

    /**
     * Properties service is part of an add-on and cannot always be injected automatically.
     */
    @Autowired(required = false)
    //@Inject(optional = true)
    private PropertiesService propertiesService;


    public RepositoryConfiguration getRepositoryConfiguration(String repoKey) {
        Repo repo = repositoryService.nonCacheRepositoryByKey(repoKey);
        if (repo == null) {
            return null;
        }
        Descriptor descriptor = repo.getDescriptor();
        if (repo.isLocal()) {
            return new LocalRepositoryConfigurationImpl((LocalRepoDescriptor) descriptor);
        } else if (repo.isReal()) {
            return new HttpRepositoryConfigurationImpl((HttpRepoDescriptor) descriptor);
        } else {
            return new VirtualRepositoryConfigurationImpl((VirtualRepoDescriptor) descriptor);
        }
    }

    public ItemInfo getItemInfo(RepoPath repoPath) {
        return repositoryService.getItemInfo(repoPath);
    }

    public FileInfo getFileInfo(RepoPath repoPath) {
        return repositoryService.getFileInfo(repoPath);
    }

    public List<ItemInfo> getChildren(RepoPath repoPath) {
        return repositoryService.getChildren(repoPath);
    }

    public String getStringContent(FileInfo fileInfo) {
        return repositoryService.getStringContent(fileInfo);
    }

    public MetadataInfo getMetadataInfo(RepoPath repoPath, String metadataName) {
        return repositoryService.getMetadataInfo(repoPath, metadataName);
    }

    public List<String> getMetadataNames(RepoPath repoPath) {
        return repositoryService.getMetadataNames(repoPath);
    }

    public String getXmlMetadata(RepoPath repoPath, String metadataName) {
        return repositoryService.getXmlMetadata(repoPath, metadataName);
    }

    public boolean hasMetadata(RepoPath repoPath, String metadataName) {
        return repositoryService.hasMetadata(repoPath, metadataName);
    }

    public void setXmlMetadata(RepoPath repoPath, String metadataName, @Nonnull String metadataContent) {
        repositoryService.setXmlMetadata(repoPath, metadataName, metadataContent);
    }

    public void removeMetadata(RepoPath repoPath, String metadataName) {
        repositoryService.removeMetadata(repoPath, metadataName);
    }

    public boolean exists(RepoPath repoPath) {
        return repositoryService.exists(repoPath);
    }

    public StatusHolder deploy(RepoPath repoPath, InputStream inputStream) {
        return repositoryService.deploy(repoPath, inputStream);
    }

    public StatusHolder undeploy(RepoPath repoPath) {
        return repositoryService.undeploy(repoPath);
    }

    public boolean isLocalRepoPathHandled(RepoPath repoPath) {
        return repositoryService.isLocalRepoPathHandled(repoPath);
    }

    public boolean isLocalRepoPathAccepted(RepoPath repoPath) {
        return repositoryService.isLocalRepoPathAccepted(repoPath);
    }

    //-- Properties

    public Properties getProperties(RepoPath repoPath) {
        return getPropertiesService().getProperties(repoPath);
    }

    public boolean hasProperty(RepoPath repoPath, String propertyName) {
        Set<String> values = getPropertyValues(repoPath, propertyName);
        return values != null && !values.isEmpty();
    }

    @Nullable
    public Set<String> getPropertyValues(RepoPath repoPath, String propertyName) {
        Properties props = getPropertiesService().getProperties(repoPath);
        Set<String> values = props.get(propertyName);
        return values;
    }

    /**
     * Get the first property value
     *
     * @param repoPath
     * @param propertyName
     * @return
     */
    public String getProperty(RepoPath repoPath, String propertyName) {
        Set<String> values = getPropertyValues(repoPath, propertyName);
        if (values != null && values.size() > 0) {
            return values.iterator().next();
        }
        return null;
    }

    public Properties setProperty(RepoPath repoPath, String propertyName, String... values) {
        PropertiesService ps = getPropertiesService();
        Property property = new Property();
        property.setName(propertyName);
        ps.addProperty(repoPath, null, property, values);
        return ps.getProperties(repoPath);
    }

    public Properties setPropertyRecursively(RepoPath repoPath, String propertyName, String... values) {
        PropertiesService ps = getPropertiesService();
        Property property = new Property();
        property.setName(propertyName);
        ps.addPropertyRecursively(repoPath, null, property, values);
        return ps.getProperties(repoPath);
    }

    public void deleteProperty(RepoPath repoPath, String propertyName) {
        getPropertiesService().deleteProperty(repoPath, propertyName);
    }

    private PropertiesService getPropertiesService() {
        if (propertiesService == null) {
            throw new RuntimeException("Using properties requires the Artifactory Pro Power Pack.");
        }
        return propertiesService;
    }

}