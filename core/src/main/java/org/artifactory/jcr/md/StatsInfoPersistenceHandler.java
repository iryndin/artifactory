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

import org.artifactory.api.fs.MetadataInfoImpl;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchControls;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchResult;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.md.MetadataInfo;
import org.artifactory.repo.jcr.JcrHelper;
import org.artifactory.search.SearcherBase;
import org.artifactory.search.xml.metadata.StatsInfoSearcher;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Calendar;

import static org.artifactory.jcr.JcrTypes.*;

/**
 * @author freds
 */
public class StatsInfoPersistenceHandler extends AbstractMetadataPersistenceHandler<StatsInfo> {

    public StatsInfoPersistenceHandler(XmlMetadataProvider<StatsInfo> xmlProvider) {
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
        StatsInfo statsInfo = new StatsInfo();
        statsInfo.setDownloadCount(
                JcrHelper.getLongProperty(metadataNode, PROP_ARTIFACTORY_STATS_DOWNLOAD_COUNT, 0L, false));
        statsInfo.setLastDownloaded(
                JcrHelper.getLongProperty(metadataNode, PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED, 0L, false));
        statsInfo.setLastDownloadedBy(
                JcrHelper.getStringProperty(metadataNode, PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED_BY, "",
                        false));
        return statsInfo;
    }

    public void update(MetadataAware metadataAware, StatsInfo statsInfo) {
        Node metadataNode = getMetadataNode(metadataAware, true);
        JcrHelper.setCalenderProperty(metadataNode, PROP_ARTIFACTORY_STATS_DOWNLOAD_COUNT,
                statsInfo.getDownloadCount());
        long value = statsInfo.getLastDownloaded();
        if (value <= 0) {
            value = System.currentTimeMillis();
        }
        JcrHelper.setDateProperty(metadataNode, PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED, value);
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
        MetadataInfo mdi = new MetadataInfoImpl(metadataAware.getRepoPath(), metadataName);
        try {
            Calendar created = metadataNode.getProperty(JcrTypes.PROP_ARTIFACTORY_CREATED).getDate();
            mdi.setCreated(created.getTimeInMillis());
            Calendar lastModified =
                    metadataNode.getProperty(PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED).getDate();
            mdi.setLastModified(lastModified.getTimeInMillis());
            String lastModifiedBy =
                    metadataNode.getProperty(PROP_ARTIFACTORY_STATS_LAST_DOWNLOADED_BY).getString();
            mdi.setLastModifiedBy(lastModifiedBy);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot get metadata info " + metadataName + " for " + metadataAware + ".", e);
        }
        return mdi;
    }

    @Override
    protected Node createMetadataNode(Node metadataContainer) {
        return JcrHelper.getOrCreateNode(metadataContainer, getMetadataName(), JcrTypes.NT_ARTIFACTORY_METADATA,
                JcrTypes.MIX_ARTIFACTORY_BASE, JcrTypes.MIX_ARTIFACTORY_STATS);
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

    public StatsInfo copy(StatsInfo original) {
        return new StatsInfo(original);
    }

    public SearcherBase<GenericMetadataSearchControls<StatsInfo>, GenericMetadataSearchResult<StatsInfo>> getSearcher() {
        return new StatsInfoSearcher();
    }
}
