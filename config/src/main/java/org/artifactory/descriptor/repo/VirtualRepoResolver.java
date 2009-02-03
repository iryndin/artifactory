package org.artifactory.descriptor.repo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolvesd recursively the search order of the virtual repositories. The resolving is done
 * according to the virtual repository resopsitories list order. Local repositories are always
 * placed first. If the virtual repo has cycles (one or more virtual repos appears more than once)
 * the resolver will skip the repeated virtual repo.
 *
 * @author Yossi Shaul
 */
public class VirtualRepoResolver {
    private final static Logger log = LoggerFactory.getLogger(VirtualRepoResolver.class);

    private List<LocalRepoDescriptor> localRepos = new ArrayList<LocalRepoDescriptor>();
    private List<RemoteRepoDescriptor> remoteRepos = new ArrayList<RemoteRepoDescriptor>();

    private boolean hasCycle = false;

    public VirtualRepoResolver(VirtualRepoDescriptor virtual) {
        resolve(virtual, new ArrayList<VirtualRepoDescriptor>());
    }

    private void resolve(VirtualRepoDescriptor virtualRepo,
            ArrayList<VirtualRepoDescriptor> visitedVirtualRepos) {
        if (visitedVirtualRepos.contains(virtualRepo)) {
            // don't visit twice the same virtual repo to prvent cycles
            log.debug("Virtual repo {} already visited.", visitedVirtualRepos);
            hasCycle = true;
            return;
        }

        visitedVirtualRepos.add(virtualRepo);
        List<RepoDescriptor> repos = virtualRepo.getRepositories();
        for (RepoDescriptor repo : repos) {
            if (repo instanceof LocalRepoDescriptor) {
                LocalRepoDescriptor localRepo = (LocalRepoDescriptor) repo;
                if (!localRepos.contains(localRepo)) {
                    localRepos.add(localRepo);
                }
            } else if (repo instanceof RemoteRepoDescriptor) {
                RemoteRepoDescriptor remoteRepo = (RemoteRepoDescriptor) repo;
                if (!remoteRepos.contains(remoteRepo)) {
                    remoteRepos.add(remoteRepo);
                }
            } else if (repo instanceof VirtualRepoDescriptor) {
                // resolve recursively
                resolve((VirtualRepoDescriptor) repo, visitedVirtualRepos);
            } else {
                log.warn("Unexpected repository of type " + repo.getClass());
            }
        }
    }

    /**
     * @return List of all the detected local repositories.
     */
    public List<LocalRepoDescriptor> getLocalRepos() {
        return localRepos;
    }

    /**
     * @return List of all the detected remote repositories.
     */
    public List<RemoteRepoDescriptor> getRemoteRepos() {
        return remoteRepos;
    }

    /**
     * @return List with all the resolved local and remote repositories ordered correctly.
     */
    public List<RealRepoDescriptor> getOrderedRepos() {
        ArrayList<RealRepoDescriptor> orderedRepos =
                new ArrayList<RealRepoDescriptor>(localRepos.size() + remoteRepos.size());
        orderedRepos.addAll(localRepos);
        orderedRepos.addAll(remoteRepos);
        return orderedRepos;
    }

    /**
     * @return True if the virtual repository contains a cycle (virtual repo that appears more than
     *         once).
     */
    public boolean hasCycle() {
        return hasCycle;
    }
}
