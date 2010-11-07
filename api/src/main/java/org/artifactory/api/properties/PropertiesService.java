/*
 * Copyright 2010 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.api.properties;

import org.artifactory.api.repo.Lock;
import org.artifactory.descriptor.property.Property;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;

import java.util.Map;
import java.util.Set;

/**
 * @author Yossi Shaul
 */
public interface PropertiesService {

    /**
     * @param repoPath The item (repository/folder/file) repository path
     * @return The properties attached to this repo path. Empty properties if none exist.
     */
    Properties getProperties(RepoPath repoPath);

    /**
     * Adds (and stores) a property to the item at the repo path.
     *
     * @param repoPath    The item repo path
     * @param propertySet Property set to add - can be null
     * @param property    Property to add
     * @param values      Property values (if null, will not add the property)
     */
    void addProperty(RepoPath repoPath, PropertySet propertySet, Property property, String... values);

    /**
     * Edit a property on a specific repo path.
     *
     * @param repoPath    The item repo path
     * @param propertySet The property set to edit
     * @param property    The property to add
     * @param values      Property values
     */
    void editProperty(RepoPath repoPath, PropertySet propertySet, Property property, String... values);


    /**
     * Recursively adds (and stores) a property to the item at the repo path.
     *
     * @param repoPath    The item repo path
     * @param propertySet Property set to add - can be null
     * @param property    Property to add
     * @param values      Property values (if null, will not add the property)
     */
    @Lock(transactional = true)
    void addPropertyRecursively(RepoPath repoPath, PropertySet propertySet, Property property, String... values);

    /**
     * Deletes the property from the item.
     *
     * @param repoPath The item repo path
     * @param property Property name to delete
     */
    void deleteProperty(RepoPath repoPath, String property);

    /**
     * Deletes property from the item recursively.
     *
     * @param repoPath The item repo path
     * @param property Property name to delete
     */
    @Lock(transactional = true)
    void deletePropertyRecursively(RepoPath repoPath, String property);

    /**
     * Returns map of properties for the given repo paths
     *
     * @param repoPaths     Paths to extract properties for
     * @param mandatoryKeys Any property keys that should be mandatory for resulting properties. If provided, property
     *                      objects will be added to the map only if they contain all the given keys
     * @return Map of repo paths with their corresponding properties
     */
    Map<RepoPath, Properties> getProperties(Set<RepoPath> repoPaths, String... mandatoryKeys);

}