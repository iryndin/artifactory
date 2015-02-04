package org.artifactory.maven;

import org.artifactory.api.maven.MavenMetadataService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.storage.fs.service.TasksService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

/**
 * A service for calculating maven metadata.
 *
 * @author Yossi Shaul
 */
@Service
public class MavenMetadataServiceImpl implements MavenMetadataService {
    private static final Logger log = LoggerFactory.getLogger(MavenMetadataServiceImpl.class);

    @Autowired
    private InternalRepositoryService repoService;
    @Autowired
    private TasksService tasksService;
    @Autowired
    private SecurityService securityService;

    // a semaphore to guard against parallel maven plugins metadata calculations
    private final Semaphore pluginsMDSemaphore = new Semaphore(1);
    // queue of repository keys that requires maven metadata plugins calculation
    private final Queue<String> pluginsMDQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void calculateMavenMetadataAsync(RepoPath baseFolderPath, boolean recursive) {
        calculateMavenMetadata(baseFolderPath, recursive);
    }

    @Override
    public void calculateMavenMetadataAsyncNonRecursive(Set<RepoPath> baseFolderPaths) {
        for (RepoPath baseFolderPath : baseFolderPaths) {
            calculateMavenMetadata(baseFolderPath, false);
        }
    }

    @Override
    public void calculateMavenMetadata(RepoPath baseFolderPath, boolean recursive) {
        log.trace("Calculate maven metadata on {}", baseFolderPath);
        LocalRepo localRepo;
        if (baseFolderPath == null) {
            log.debug("Cannot calculate Maven metadata for a null path ");
            return;
        }
        localRepo = repoService.localRepositoryByKey(baseFolderPath.getRepoKey());
        if (localRepo == null) {
            log.debug("Couldn't find local non-cache repository for path '{}'.", baseFolderPath);
            return;
        }

        if (!localRepo.itemExists(baseFolderPath.getPath())) {
            log.debug("Couldn't find path '{}'.", baseFolderPath);
            return;
        }

        new MavenMetadataCalculator(baseFolderPath, recursive).calculate();

        // Calculate maven plugins metadata asynchronously
        getTransactionalMe().calculateMavenPluginsMetadataAsync(localRepo.getKey());
    }

    @Override
    public void calculateMavenPluginsMetadataAsync(String repoKey) {

        if (pluginsMDQueue.contains(repoKey)) {
            log.debug("Plugins maven metadata calculation for repo '{}' already waiting in queue", repoKey);
            return;
        }

        // add the repository key to the queue (there's a small chance that the same key will be added twice but it
        // doesn't worth locking again)
        log.debug("Adding '{}' to the plugins maven metadata calculation queue", repoKey);
        pluginsMDQueue.add(repoKey);

        // try to acquire the single lock to do the metadata calculation. If we don't get it, another thread is already
        // performing the job and it will also do the one just added to the queue
        if (!pluginsMDSemaphore.tryAcquire()) {
            log.debug("Plugins maven metadata calculation already running in another thread");
            return;
        }

        // ok i'm in, lets perform all the job in the queue
        try {
            String repoToCalculate;
            while ((repoToCalculate = pluginsMDQueue.poll()) != null) {
                log.debug("Calculating plugins maven metadata for {}.", repoToCalculate);
                try {
                    LocalRepo localRepo =
                            localRepositoryByKeyFailIfNull(InternalRepoPathFactory.repoRootPath(repoToCalculate));
                    MavenPluginsMetadataCalculator calculator = new MavenPluginsMetadataCalculator();
                    calculator.calculate(localRepo);
                } catch (Exception e) {
                    log.error("Failed to calculate plugin maven metadata on repo '" + repoToCalculate + "':", e);
                }
            }
        } finally {
            pluginsMDSemaphore.release();
        }
    }

    // get all folders marked for maven metadata calculation and execute the metadata calculation

    private LocalRepo localRepositoryByKeyFailIfNull(RepoPath localRepoPath) {
        LocalRepo localRepo = repoService.localRepositoryByKey(localRepoPath.getRepoKey());
        if (localRepo == null) {
            throw new IllegalArgumentException("Couldn't find local non-cache repository for path " + localRepoPath);
        }
        return localRepo;
    }

    private static MavenMetadataService getTransactionalMe() {
        return InternalContextHelper.get().beanForType(MavenMetadataService.class);
    }
}
