/*
 * Copyright 2012 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.api.properties;

import org.artifactory.descriptor.property.Property;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.Lock;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * @author Chen  Keinan
 */
public interface LockableAddProperties {

    /**
     * Recursively adds (and stores) a property to the item at the repo path.
     *
     * @param repoPath    The item repo path
     * @param propertySet Property set to add - can be null
     * @param propertyMapFromRequests    Property map from request
     */
    @Lock
    void addPropertyInternalMultiple(RepoPath repoPath, @Nullable PropertySet propertySet,
                                     Map<Property, List<String>> propertyMapFromRequests);

    @Lock
    void addSha256PropertyInternalMultiple(RepoPath repoPath);
}