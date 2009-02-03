package org.artifactory.repo;

import org.apache.log4j.Logger;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrFile;
import org.artifactory.jcr.JcrFolder;
import org.artifactory.jcr.JcrFsItem;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.resource.NotFoundRepoResource;
import org.artifactory.resource.RepoResource;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.List;

public class LocalCacheRepo extends JcrRepo {
    private static final Logger LOGGER = Logger.getLogger(LocalCacheRepo.class);

    public static final String PATH_SUFFIX = "-cache";

    private RemoteRepo remoteRepo;

    public LocalCacheRepo(RemoteRepo remoteRepo, CentralConfig cc) {
        assert remoteRepo != null;
        this.remoteRepo = remoteRepo;
        setDescription(remoteRepo.getDescription() + " (local file cache)");
        setKey(remoteRepo.getKey() + PATH_SUFFIX);
        init(cc);
    }

    @Override
    public RepoResource getInfo(final String path) {
        RepoResource repoResource = super.getInfo(path);
        if (repoResource.isFound()) {
            //Check that the item has not expired yet
            boolean expired = isExpired(repoResource);
            if (expired) {
                //Return not found
                repoResource = new NotFoundRepoResource(path, this);
            }
        }
        return repoResource;
    }

    @Override
    public boolean isCache() {
        return true;
    }

    public void undeploy(String path) {
        //Undeploy all nodes recursively
        int itemsEffected = processSubNodes(path, true);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Removed '" + path + "' from local cache: " + itemsEffected +
                    " items effected.");
        }
    }

    public void unexpire(final String path) {
        //Reset the resource age
        JcrHelper jcr = getJcr();
        JcrFile file = jcr.doInSession(
                new JcrCallback<JcrFile>() {
                    public JcrFile doInJcr(JcrSessionWrapper session)
                            throws RepositoryException {
                        return (JcrFile) getFsItem(path, session);
                    }
                });
        file.setLastUpdatedTime(System.currentTimeMillis());
    }

    public void expire(String path) {
        //Zap all nodes recursively from all retrieval caches
        int itemsEffected = processSubNodes(path, false);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Zapped '" + path + "' from local cache: " + itemsEffected +
                    " items effected.");
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private int processSubNodes(final String path, final boolean remove) {
        JcrHelper jcr = getJcr();
        Integer itemsEffected = jcr.doInSession(new JcrCallback<Integer>() {
            @SuppressWarnings({"UnnecessaryLocalVariable"})
            public Integer doInJcr(JcrSessionWrapper session) throws RepositoryException {
                Node repoNode = getRepoJcrNode(session);
                Node folderNode = repoNode.getNode(path);
                JcrFolder folder = new JcrFolder(folderNode);
                int itemsCount = processSubNodes(folder, 0, remove);
                return itemsCount;

            }
        });
        return itemsEffected;
    }

    private int processSubNodes(JcrFolder folder, int itemsCount, boolean remove) {
        List<JcrFsItem> list = folder.getItems();
        for (JcrFsItem item : list) {
            if (item.isDirectory()) {
                itemsCount = processSubNodes((JcrFolder) item, itemsCount, remove);
            } else {
                //Remove from remote repo caches
                String path = item.relPath();
                remoteRepo.removeFromCaches(path);
                if (remove) {
                    //Remove the node from the cache repo
                    item.remove();
                } else {
                    //Effectively force expiry on the file by changing it's lastUpdated time
                    JcrFile file = (JcrFile) item;
                    long retrievalCahePeriodMillis =
                            remoteRepo.getRetrievalCachePeriodSecs() * 1000;
                    file.setLastUpdatedTime(System.currentTimeMillis() - retrievalCahePeriodMillis);
                }
                itemsCount++;
            }
        }
        return itemsCount;
    }

    private boolean isExpired(RepoResource repoResource) {
        long retrievalCahePeriodMillis = remoteRepo.getRetrievalCachePeriodSecs() * 1000;
        long age = repoResource.getAge();
        return age > retrievalCahePeriodMillis || age == -1;
    }
}