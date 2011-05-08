/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.repo;

import org.artifactory.common.StatusHolder;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.ItemInfo;
import org.artifactory.md.MetadataInfo;
import org.artifactory.md.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * Public API for working with repositories
 */
public interface Repositories {

    RepositoryConfiguration getRepositoryConfiguration(String repoKey);

    /**
     * @param repoPath Repository path of the item
     * @return Folder or file info. Throws exception if the path doesn't exist.
     */
    ItemInfo getItemInfo(RepoPath repoPath);

    /**
     * @param repoPath Repository path of the file
     * @return The file info. Throws exception if the path doesn't exist or it doesn't point to a file.
     */
    FileInfo getFileInfo(RepoPath repoPath);

    List<ItemInfo> getChildren(RepoPath repoPath);

    String getStringContent(FileInfo fileInfo);

    /**
     * @param repoPath     Repository path of the metadata aware item
     * @param metadataName The metadata name
     * @return The metadata info. Returns null if not found.
     */
    @Nullable
    MetadataInfo getMetadataInfo(RepoPath repoPath, String metadataName);

    /**
     * Returns the available metadata names which are not internal
     *
     * @param repoPath The full path of the object having metadata
     * @return A list of metadata names that exists on this element
     */
    List<String> getMetadataNames(RepoPath repoPath);

    /**
     * Returns the metadata of the given name.
     *
     * @param repoPath     A repo path (usually pointing to an JcrFsItem)
     * @param metadataName Name of metadata to return. Cannot be null
     * @return Requested metadata if found. Null if not
     * @throws IllegalArgumentException If given a blank metadata name
     */
    String getXmlMetadata(RepoPath repoPath, String metadataName);

    /**
     * Indicates whether this item adorns the given metadata
     *
     * @param repoPath     A repo path (usually pointing to an JcrFsItem)
     * @param metadataName Name of metadata to locate
     * @return True if annotated by the given metadata. False if not
     * @throws IllegalArgumentException If given a blank metadata name
     */
    boolean hasMetadata(RepoPath repoPath, String metadataName);

    /**
     * Sets the given metadata on the supplied repo path.
     *
     * @param repoPath        Path to targeted item
     * @param metadataName    The metadata name to set under
     * @param metadataContent The metadata content to add. Cannot be null
     * @throws IllegalArgumentException When given a null metadata value
     */
    void setXmlMetadata(RepoPath repoPath, String metadataName, @Nonnull String metadataContent);

    /**
     * Removes the metadata of the given name
     *
     * @param repoPath     Path to targeted item
     * @param metadataName Name of metadata to remove
     */
    void removeMetadata(RepoPath repoPath, String metadataName);

    Properties getProperties(RepoPath repoPath);

    boolean hasProperty(RepoPath repoPath, String propertyName);

    public Set<String> getPropertyValues(RepoPath repoPath, String propertyName);

    public String getProperty(RepoPath repoPath, String propertyName);

    Properties setProperty(RepoPath repoPath, String propertyName, String... values);

    Properties setPropertyRecursively(RepoPath repoPath, String propertyName, String... values);

    /**
     * Deletes the property from the item.
     *
     * @param repoPath     The item repo path
     * @param propertyName Property name to delete
     */
    void deleteProperty(RepoPath repoPath, String propertyName);


    boolean exists(RepoPath repoPath);

    /**
     * Deploy an artifact
     *
     * @param repoPath
     * @param inputStream
     * @return
     */
    StatusHolder deploy(RepoPath repoPath, InputStream inputStream);

    StatusHolder undeploy(RepoPath repoPath);

    boolean isLocalRepoPathHandled(RepoPath repoPath);

    boolean isLocalRepoPathAccepted(RepoPath repoPath);
}
