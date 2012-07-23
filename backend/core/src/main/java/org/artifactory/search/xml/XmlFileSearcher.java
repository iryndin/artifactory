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

package org.artifactory.search.xml;

import com.google.common.collect.Lists;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.xml.XmlSearchResult;
import org.artifactory.api.search.xml.metadata.MetadataSearchControls;
import org.artifactory.fs.FileInfo;
import org.artifactory.jcr.factory.VfsItemFactory;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.PathFactory;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.data.VfsNodeType;
import org.artifactory.sapi.search.VfsQueryResult;
import org.artifactory.sapi.search.VfsRepoQuery;

import java.util.List;

import static org.artifactory.storage.StorageConstants.NODE_ARTIFACTORY_XML;

/**
 * Holds the xml files search logic
 *
 * @author Noam Tenne
 */
public class XmlFileSearcher extends XmlSearcherBase<XmlSearchResult> {

    @Override
    protected void appendMetadataPath(VfsRepoQuery query, String metadataName) {
        query.addPathFilter(metadataName).setNodeTypeFilter(VfsNodeType.FILE);
        query.addPathFilters(NODE_ARTIFACTORY_XML);
    }

    @Override
    protected ItemSearchResults<XmlSearchResult> filterAndReturnResults(
            MetadataSearchControls controls, VfsQueryResult queryResult) {

        List<XmlSearchResult> results = Lists.newArrayList();

        boolean limit = controls.isLimitSearchResults();
        PathFactory pathFactory = PathFactoryHolder.get();
        for (VfsNode vfsNode : queryResult.getNodes()) {
            if (limit && results.size() >= getMaxResults()) {
                break;
            }
            String path = vfsNode.absolutePath();
            String metadataNameFromPath = NamingUtils.getMetadataNameFromJcrPath(path);
            //Node path might not be an xml path (if user has searched for "/" in the path with a blank value)
            int xmlNodeIndex = path.lastIndexOf("/" + NODE_ARTIFACTORY_XML);
            if (xmlNodeIndex > 0) {
                path = path.substring(0, xmlNodeIndex);
            }

            RepoPath repoPath = pathFactory.getRepoPath(path);
            LocalRepo localRepo = getRepoService().localOrCachedRepositoryByKey(repoPath.getRepoKey());
            if (localRepo == null || !isResultAcceptable(repoPath, localRepo)) {
                continue;
            }

            FileInfo fileInfo = VfsItemFactory.createFileInfoProxy(repoPath);
            results.add(new XmlSearchResult(fileInfo, metadataNameFromPath));
        }
        return new ItemSearchResults<XmlSearchResult>(results, queryResult.getCount());
    }
}