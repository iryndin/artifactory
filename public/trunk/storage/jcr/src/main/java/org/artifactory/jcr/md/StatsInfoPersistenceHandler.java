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
import org.artifactory.factory.BasicFactory;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.MutableStatsInfo;
import org.artifactory.fs.StatsInfo;
import org.artifactory.jcr.utils.JcrHelper;
import org.artifactory.md.MetadataInfo;
import org.artifactory.md.MutableMetadataInfo;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.storage.StorageConstants;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Calendar;

import static org.artifactory.storage.StorageConstants.*;

/**
 * @author freds
 */
public class StatsInfoPersistenceHandler extends AbstractMetadataPersistenceHandler<StatsInfo, MutableStatsInfo> {

    public StatsInfoPersistenceHandler(XmlMetadataProvider<StatsInfo, MutableStatsInfo> xmlProvider) {
        super(xmlProvider);
    }

    @Override
    protected String getXml(MetadataAware metadataAware) {
        return getXmlProvider().toXml(read(metadataAware));
    }

    public StatsInfo read(MetadataAware metadataAware) {
        Node metadataNode = getMetadataNode(metadataAware, false);
        if (metadataNode == null) {
            return null;
        }
        MutableStatsInfo statsInfo = InfoFactoryHolder.get().createStats();
        statsInfo.setDownloadCount(
                JcrHelper.getLongProperty(metadataNode, PROP_ARTIFACTORY_STATS_DOWNLOAD_COUNT, 0L, false));
        statsInfo.setLastDownloaded(
                JcrHelper.getLongProperty(metadataNode, PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED, 0L, false));
        statsInfo.setLastDownloadedBy(
                JcrHelper.getStringProperty(metadataNode, PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED_BY, "", false));
        return statsInfo;
    }

    public void update(MetadataAware metadataAware, MutableStatsInfo statsInfo) {
        Node metadataNode = getMetadataNode(metadataAware, true);
        JcrHelper.setLongProperty(metadataNode, PROP_ARTIFACTORY_STATS_DOWNLOAD_COUNT, statsInfo.getDownloadCount());

        long lastDownloaded = statsInfo.getLastDownloaded();
        JcrHelper.setDateProperty(metadataNode, PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED,
                (lastDownloaded == 0) ? null : lastDownloaded);
        String lastDownloadedBy = statsInfo.getLastDownloadedBy();
        if (lastDownloadedBy == null) {
            lastDownloadedBy = getAuthorizationService().currentUsername();
        }
        JcrHelper.setStringProperty(metadataNode, PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED_BY, lastDownloadedBy);
    }

    public MetadataInfo getMetadataInfo(MetadataAware metadataAware) {
        Node metadataNode = getMetadataNode(metadataAware, false);
        if (metadataNode == null) {
            return null;
        }
        String metadataName = getMetadataName();
        MutableMetadataInfo mdi = InfoFactoryHolder.get().createMetadata(metadataAware.getRepoPath(), metadataName);
        try {
            Calendar created = metadataNode.getProperty(StorageConstants.PROP_ARTIFACTORY_CREATED).getDate();
            mdi.setCreated(created.getTimeInMillis());
            mdi.setLastModified(JcrHelper.getLongProperty(metadataNode, PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED, 0L,
                    true));
            mdi.setLastModifiedBy(metadataNode.getProperty(PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED_BY).getString());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot get metadata info " + metadataName + " for " + metadataAware + ".", e);
        }
        return mdi;
    }

    @Override
    protected Node createMetadataNode(Node metadataContainer) {
        return JcrHelper.getOrCreateNode(metadataContainer, getMetadataName(), StorageConstants.NT_ARTIFACTORY_METADATA,
                StorageConstants.MIX_ARTIFACTORY_BASE, StorageConstants.MIX_ARTIFACTORY_STATS);
    }

    public void remove(MetadataAware metadataAware) {
        try {
            Node metadataNode = getMetadataNode(metadataAware, false);
            if (metadataNode != null) {
                metadataNode.remove();
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to clear node's statistics metadata for " + metadataAware, e);
        }
    }

    public MutableStatsInfo copy(StatsInfo original) {
        return InfoFactoryHolder.get().copyStats(original);
    }

    public Searcher<GenericMetadataSearchControls<StatsInfo>, GenericMetadataSearchResult<StatsInfo>> getSearcher() {
        //return new GenericStatsInfoSearcher(); Commented out in the meanwhile because it's unnecessarily complex
        return (Searcher<GenericMetadataSearchControls<StatsInfo>, GenericMetadataSearchResult<StatsInfo>>)
                BasicFactory.createInstance(Searcher.class,
                        "org.artifactory.search.xml.metadata.LastDownloadedSearcher");
    }
}
