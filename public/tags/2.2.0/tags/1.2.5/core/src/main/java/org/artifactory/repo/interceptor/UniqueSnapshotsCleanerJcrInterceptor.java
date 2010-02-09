package org.artifactory.repo.interceptor;

import org.apache.commons.collections15.SortedBag;
import org.apache.commons.collections15.bag.TreeBag;
import org.apache.log4j.Logger;
import org.artifactory.jcr.JcrFile;
import org.artifactory.jcr.JcrFolder;
import org.artifactory.jcr.JcrFsItem;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.resource.RepoResource;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class UniqueSnapshotsCleanerJcrInterceptor implements LocalRepoInterceptor {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER =
            Logger.getLogger(UniqueSnapshotsCleanerJcrInterceptor.class);


    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public InputStream beforeResourceSave(RepoResource res, LocalRepo repo, InputStream in)
            throws Exception {
        return in;
    }

    @SuppressWarnings({"OverlyComplexMethod", "ConstantConditions"})
    public void afterResourceSave(final RepoResource res, final LocalRepo repo) throws Exception {
        String path = res.getPath();
        JcrFile file = (JcrFile) repo.getFsItem(path);
        if (!MavenUtils.isSnapshotMetadata(path)) {
            return;
        }
        int maxUniqueSnapshots = repo.getMaxUniqueSnapshots();
        if (maxUniqueSnapshots > 0) {
            //Read the last updated and delete old unique snapshot artifacts
            JcrFolder snapshotFolder = file.getParent();
            List<JcrFsItem> children = snapshotFolder.getItems();
            List<ItemDesc> itemsByDate = new ArrayList<ItemDesc>();
            for (JcrFsItem child : children) {
                String name = child.getName();
                Matcher m = MavenUtils.UNIQUE_SNAPSHOT_NAME_PATTERN.matcher(name);
                if (m.matches()) {
                    String childTimeStamp = m.group(3);
                    Date childLastUpdated = MavenUtils.timestampToDate(childTimeStamp);
                    //Add it to the sorted set - newer items closer to the head
                    ItemDesc newDesc = new ItemDesc(child, childLastUpdated);
                    itemsByDate.add(newDesc);
                }
            }
            Collections.sort(itemsByDate);
            //Traverse the ordered collection and stop when we collected enough items
            //(maxUniqueSnapshots)
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
            for (ItemDesc item : itemsByDate) {
                item.item.delete();
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Removed old unique snapshot '" + item.item.relPath() + "'.");
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

        public int hashCode() {
            int result;
            result = item.hashCode();
            result = 31 * result + lastModified.hashCode();
            return result;
        }
    }
}
