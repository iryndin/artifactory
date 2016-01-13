package org.artifactory.repo.service.trash.prune;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.aql.AqlService;
import org.artifactory.aql.api.domain.sensitive.AqlApiItem;
import org.artifactory.aql.result.AqlEagerResult;
import org.artifactory.aql.result.rows.AqlBaseFullRowImpl;
import org.artifactory.aql.result.rows.AqlItem;
import org.artifactory.aql.util.AqlUtils;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.trash.TrashService;
import org.artifactory.storage.binstore.GarbageCollectorInfo;
import org.artifactory.storage.binstore.service.BinaryData;
import org.artifactory.storage.binstore.service.GarbageCollectorListener;
import org.artifactory.util.TimeUnitFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static org.artifactory.aql.api.internal.AqlBase.and;

/**
 * @author Shay Yaakov
 */
public class TrashcanPruner implements GarbageCollectorListener {
    private static final Logger log = LoggerFactory.getLogger(TrashcanPruner.class);

    private final AqlService aqlService;
    private final RepositoryService repoService;
    private final CentralConfigService centralConfigService;

    public TrashcanPruner() {
        this.aqlService = ContextHelper.get().beanForType(AqlService.class);
        this.repoService = ContextHelper.get().beanForType(RepositoryService.class);
        this.centralConfigService = ContextHelper.get().getCentralConfig();
    }

    @Override
    public void start() {
        pruneTrashItems();
    }

    @Override
    public void toDelete(Collection<BinaryData> binsToDelete) {

    }

    @Override
    public void finished(GarbageCollectorInfo result) {

    }

    @Override
    public void destroy() {

    }

    private void pruneTrashItems() {
        // No action if retention is 0 (or less)
        int retentionPeriodDays = centralConfigService.getDescriptor().getTrashcanConfig().getRetentionPeriodDays();
        if (retentionPeriodDays <= 0) {
            return;
        }

        long start = System.currentTimeMillis();
        long validFrom = start - TimeUnit.MILLISECONDS.convert(retentionPeriodDays, TimeUnit.DAYS);
        log.debug("Starting trashcan pruning with valid from date: '{}'", validFrom);

        AqlEagerResult<AqlItem> trashItems = findTrashItems();
        trashItems.getResults().forEach(item -> deleteItemIfOld(item, validFrom));

        long end = System.currentTimeMillis();
        String duration = TimeUnitFormat.getTimeString((end - start), TimeUnit.MILLISECONDS);
        log.info("Trashcan pruning total execution time: '{}'", duration);
    }

    private AqlEagerResult<AqlItem> findTrashItems() {
        AqlApiItem query = AqlApiItem.create().filter(and(
                AqlApiItem.repo().equal(TrashService.TRASH_KEY),
                AqlApiItem.property().key().equal(TrashService.PROP_TRASH_TIME)));

        return aqlService.executeQueryEager(query);
    }

    private void deleteItemIfOld(AqlItem item, long validFrom) {
        RepoPath repoPath = AqlUtils.fromAql((AqlBaseFullRowImpl) item);
        Properties itemProperties = repoService.getProperties(repoPath);
        if (itemProperties == null) {
            log.debug("Unable to find properties for '{}'", repoPath);
            return;
        }

        String trashTimeProperty = itemProperties.getFirst(TrashService.PROP_TRASH_TIME);
        if (StringUtils.isBlank(trashTimeProperty)) {
            log.warn("Unable to find trash.time property for '{}'", repoPath);
            return;
        }

        try {
            long trashTime = Long.parseLong(trashTimeProperty);
            if (trashTime < validFrom) {
                log.debug("Removing old trash item '{}' with trash time of '{}'", repoPath, trashTime);
                repoService.undeploy(repoPath, false, false);
            }
        } catch (Exception e) {
            log.error("Error during deletion of the old trash item '{}': '{}'", repoPath, e.getMessage());
            log.debug("Error during deletion old trash item: " + e.getMessage(), e);
        }
    }
}
