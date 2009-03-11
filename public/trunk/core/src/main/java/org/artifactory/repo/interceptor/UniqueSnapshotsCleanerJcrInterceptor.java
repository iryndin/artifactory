/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.repo.interceptor;

import org.apache.commons.collections15.SortedBag;
import org.apache.commons.collections15.bag.TreeBag;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.resource.RepoResource;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class UniqueSnapshotsCleanerJcrInterceptor implements LocalRepoInterceptor {
    private static final Logger log =
            LoggerFactory.getLogger(UniqueSnapshotsCleanerJcrInterceptor.class);


    public InputStream beforeResourceSave(RepoResource res, LocalRepo repo, InputStream in)
            throws Exception {
        return in;
    }

    public void afterResourceSave(final RepoResource res, final LocalRepo repo) throws Exception {
        String path = res.getRepoPath().getPath();
        JcrFile file = repo.getLockedJcrFile(path, true);
        if (!MavenNaming.isUniqueSnapshot(path) || MavenNaming.isPom(path)) {
            return;
        }
        int maxUniqueSnapshots = repo.getMaxUniqueSnapshots();
        if (maxUniqueSnapshots > 0) {
            //Read the last updated and delete old unique snapshot artifacts
            JcrFolder snapshotFolder = file.getParentFolder();
            JcrRepoService jcrRepoService = InternalContextHelper.get().getJcrRepoService();
            List<JcrFsItem> children = jcrRepoService.getChildren(snapshotFolder, true);
            List<ItemDesc> itemsByDate = new ArrayList<ItemDesc>();
            for (JcrFsItem child : children) {
                String name = child.getName();
                if (MavenNaming.isUniqueSnapshotFileName(name)) {
                    String childTimeStamp = MavenNaming.getUniqueSnapshotVersionTimestamp(name);
                    Date childLastUpdated = MavenUtils.timestampToDate(childTimeStamp);
                    //Add it to the sorted set - newer items closer to the head
                    ItemDesc newDesc = new ItemDesc(child, childLastUpdated);
                    itemsByDate.add(newDesc);
                }
            }
            Collections.sort(itemsByDate);
            //Traverse the ordered collection and stop when we collected enough items (maxUniqueSnapshots)
            int uniqueSnapshotsCount = 0;
            SortedBag<ItemDesc> itemsToKeep = new TreeBag<ItemDesc>();
            for (ItemDesc item : itemsByDate) {
                if (itemsToKeep.size() == 0 || item.compareTo(itemsToKeep.last()) != 0) {
                    uniqueSnapshotsCount++;
                }
                itemsToKeep.add(item);
                if (uniqueSnapshotsCount == maxUniqueSnapshots) {
                    break;
                }
            }
            itemsByDate.removeAll(itemsToKeep);
            for (ItemDesc itemDesc : itemsByDate) {
                itemDesc.item.bruteForceDelete();
                if (log.isInfoEnabled()) {
                    log.info("Removed old unique snapshot '" + itemDesc.item.getRelativePath() + "'.");
                }
            }
        }
    }

    private static class ItemDesc implements Comparable<ItemDesc>, Serializable {
        final JcrFsItem item;
        final Date lastModified;

        ItemDesc(JcrFsItem item, Date lastModified) {
            this.item = item;
            this.lastModified = lastModified;
        }

        public int compareTo(ItemDesc o) {
            return -1 * lastModified.compareTo(o.lastModified);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ItemDesc)) {
                return false;
            }
            ItemDesc itemDesc = (ItemDesc) o;
            return item.equals(itemDesc.item) && lastModified.equals(itemDesc.lastModified);

        }

        @Override
        public int hashCode() {
            int result;
            result = item.hashCode();
            result = 31 * result + lastModified.hashCode();
            return result;
        }
    }
}
