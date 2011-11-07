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

package org.artifactory.jcr.md;

import org.artifactory.api.search.Searcher;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchControls;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchResult;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.fs.ItemInfo;
import org.artifactory.fs.MutableItemInfo;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.storage.StorageConstants;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Calendar;

import static org.artifactory.jcr.utils.JcrHelper.*;
import static org.artifactory.storage.StorageConstants.*;
import static org.artifactory.util.PathUtils.hasText;

/**
 * @author freds
 */
public abstract class AbstractPersistenceHandler<T, MT> implements MetadataPersistenceHandler<T, MT> {

    private JcrService jcr;
    private AuthorizationService authService;
    private final XmlMetadataProvider<T, MT> xmlProvider;

    protected AbstractPersistenceHandler(XmlMetadataProvider<T, MT> xmlProvider) {
        this.xmlProvider = xmlProvider;
    }

    protected XmlMetadataProvider<T, MT> getXmlProvider() {
        return xmlProvider;
    }

    protected String getMetadataName() {
        return xmlProvider.getMetadataName();
    }

    protected JcrService getJcr() {
        if (jcr == null) {
            jcr = StorageContextHelper.get().getJcrService();
        }
        return jcr;
    }

    protected AuthorizationService getAuthorizationService() {
        if (authService == null) {
            authService = StorageContextHelper.get().getAuthorizationService();
        }
        return authService;
    }

    protected void markModified(Node metadataNode) {
        markModified(metadataNode, Calendar.getInstance());
    }

    protected void markModified(Node metadataNode, Calendar lastModified) {
        try {
            //Update the last modified on the specific metadata
            metadataNode.setProperty(StorageConstants.PROP_ARTIFACTORY_LAST_MODIFIED, lastModified);
            metadataNode.setProperty(StorageConstants.PROP_ARTIFACTORY_LAST_MODIFIED_BY,
                    getAuthorizationService().currentUsername());
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to set modified properties on metadata node '" + metadataNode + "'", e);
        }
    }

    public Searcher<GenericMetadataSearchControls<T>, GenericMetadataSearchResult<T>> getSearcher() {
        // TODO
        return null;
    }

    protected void fillItemInfoFromNode(Node node, MutableItemInfo itemInfo) {
        checkArtifactoryName(node, itemInfo.getName());
        itemInfo.setCreated(getLongProperty(node, PROP_ARTIFACTORY_CREATED, getJcrCreated(node), false));
        itemInfo.setLastModified(
                getLongProperty(node, PROP_ARTIFACTORY_LAST_MODIFIED, getJcrLastModified(node), false));
        itemInfo.setCreatedBy(getStringProperty(node, PROP_ARTIFACTORY_CREATED_BY, itemInfo.getCreatedBy(), true));
        itemInfo.setModifiedBy(
                getStringProperty(node, PROP_ARTIFACTORY_LAST_MODIFIED_BY, itemInfo.getModifiedBy(), true));
        itemInfo.setLastUpdated(getLongProperty(node, PROP_ARTIFACTORY_LAST_UPDATED, itemInfo.getLastUpdated(), true));
    }

    protected void setPropertiesInNodeFromInfo(Node node, ItemInfo itemInfo) {
        // Created managed by JCR only
        setCalenderProperty(node, PROP_ARTIFACTORY_CREATED, itemInfo.getCreated());
        //Set the name property for indexing and speedy searches
        setArtifactoryName(node, itemInfo.getName());
        setJcrLastModified(node, itemInfo.getLastModified());
        setCalenderProperty(node, PROP_ARTIFACTORY_LAST_MODIFIED, itemInfo.getLastModified());
        String createdBy = itemInfo.getCreatedBy();
        String modifiedBy = itemInfo.getModifiedBy();
        if (!hasText(modifiedBy)) {
            modifiedBy = getAuthorizationService().currentUsername();
        }
        if (!hasText(createdBy)) {
            createdBy = modifiedBy;
        }
        setStringProperty(node, PROP_ARTIFACTORY_CREATED_BY, createdBy);
        setStringProperty(node, PROP_ARTIFACTORY_LAST_MODIFIED_BY, modifiedBy);
        setCalenderProperty(node, PROP_ARTIFACTORY_LAST_UPDATED, itemInfo.getLastUpdated());
    }
}
