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

package org.artifactory.search.xml.metadata;

import com.google.common.collect.Lists;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.xml.metadata.MetadataSearchControls;
import org.artifactory.api.search.xml.metadata.MetadataSearchResult;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.md.PropertiesInfo;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.PathFactory;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.search.VfsQueryPathCriterion;
import org.artifactory.sapi.search.VfsQueryResult;
import org.artifactory.sapi.search.VfsRepoQuery;
import org.artifactory.search.xml.XmlSearcherBase;

import java.util.List;

import static org.artifactory.storage.StorageConstants.*;

/**
 * @author Yoav Landman
 */
public class MetadataSearcher extends XmlSearcherBase<MetadataSearchResult> {

    // /repositories/libs-releases-local/g1/g2/a/v/a-v.pom/artifactory:metadata/qa/artifactory:xml/qa/builds/build[2]/result/jcr:xmltext
    // /repositories/libs-releases-local/g1/g2/a/v/a-v.pom/artifactory:metadata/props/artifactory:properties/properties/prop1/val1/jcr:xmltext

    @Override
    protected void appendMetadataPath(VfsRepoQuery query, String metadataName) {
        query.addAllSubPathFilter();
        if ("*".equals(metadataName)) {
            // TODO: Find why we need this
            metadataName = VfsQueryPathCriterion.ALL_PATH_VALUE;
        }
        query.addPathFilter(NODE_ARTIFACTORY_METADATA);
        query.addMetadataNameFilter(metadataName);
        query.addPathFilter(
                PropertiesInfo.ROOT.equals(metadataName) ? NODE_ARTIFACTORY_PROPERTIES : NODE_ARTIFACTORY_XML);
    }

    @Override
    protected ItemSearchResults<MetadataSearchResult> filterAndReturnResults(
            MetadataSearchControls controls, VfsQueryResult queryResult) {
        boolean limit = controls.isLimitSearchResults();
        PathFactory pathFactory = PathFactoryHolder.get();
        List<MetadataSearchResult> results = Lists.newArrayList();
        for (VfsNode vfsNode : queryResult.getNodes()) {
            if (limit && results.size() >= getMaxResults()) {
                break;
            }
            String path = vfsNode.absolutePath();
            String metadataNameFromPath = NamingUtils.getMetadataNameFromJcrPath(path);
            String artifactPath = path.substring(0, path.lastIndexOf("/" + NODE_ARTIFACTORY_METADATA));

            RepoPath repoPath = pathFactory.getRepoPath(artifactPath);
            LocalRepo localRepo = getRepoService().localOrCachedRepositoryByKey(repoPath.getRepoKey());
            if (localRepo == null || !isResultAcceptable(repoPath, localRepo)) {
                continue;
            }
            JcrFsItem jcrFsItem = localRepo.getJcrFsItem(repoPath);
            Object xmlMetadataObject = null;
            Class mdObjectClass = controls.getMetadataObjectClass();
            if (mdObjectClass != null) {
                //noinspection unchecked
                xmlMetadataObject = jcrFsItem.getMetadata(mdObjectClass);
            }
            results.add(new MetadataSearchResult(jcrFsItem.getInfo(), metadataNameFromPath, xmlMetadataObject));
            //Release the read locks early
            LockingHelper.releaseReadLock(repoPath);
        }
        return new ItemSearchResults<MetadataSearchResult>(results, queryResult.getCount());
    }
}