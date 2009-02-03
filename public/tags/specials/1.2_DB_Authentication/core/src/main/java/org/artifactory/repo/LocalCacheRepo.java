package org.artifactory.repo;

import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.resource.NotFoundRepoResource;
import org.artifactory.resource.RepoResource;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class LocalCacheRepo extends JcrRepo {

    public static final String PATH_SUFFIX = "-cache";

    private RemoteRepo remoteRepo;

    public LocalCacheRepo(RemoteRepo remoteRepo, CentralConfig cc) {
        assert remoteRepo != null;
        this.remoteRepo = remoteRepo;
        setDescription(remoteRepo.getDescription() + " (local file cache)");
        setKey(remoteRepo.getKey() + PATH_SUFFIX);
        init(cc);
    }

    public RepoResource getInfo(final String path) {
        RepoResource repoResource = super.getInfo(path);
        if (repoResource.isFound()) {
            //Check that the item has not expired yet
            long retrievalCahePeriodMillis = remoteRepo.getRetrievalCachePeriodSecs() * 1000;
            long age = repoResource.getAge();
            if (age > retrievalCahePeriodMillis) {
                //Remove from local cache repo and return not found
                JcrHelper jcr = getJcr();
                jcr.doInSession(new JcrCallback<Node>() {
                    public Node doInJcr(JcrSessionWrapper session) throws RepositoryException {
                        Node repoNode = getRepoJcrNode(session);
                        Node node = repoNode.getNode(path);
                        node.remove();
                        return node;
                    }
                });
                repoResource = new NotFoundRepoResource(path, this);
            }
        }
        return repoResource;
    }
}