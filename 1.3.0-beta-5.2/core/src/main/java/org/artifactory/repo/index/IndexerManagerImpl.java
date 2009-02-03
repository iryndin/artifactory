package org.artifactory.repo.index;

import org.apache.log4j.Logger;
import org.artifactory.config.CentralConfigServiceImpl;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.PostInitializingBean;
import org.artifactory.utils.ExceptionUtils;
import org.quartz.CronExpression;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.CronTriggerBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.net.SocketTimeoutException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@Service
public class IndexerManagerImpl implements PostInitializingBean, IndexerManager {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(IndexerManagerImpl.class);

    private static final String INDEXER_TRIGGER_NAME = "indexerTrigger";

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private CentralConfigServiceImpl centralConfig;

    @Autowired
    private InternalRepositoryService repositoryService;

    private IndexerDescriptor descriptor;

    @PostConstruct
    public void register() {
        InternalContextHelper.get().addPostInit(getClass());
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends PostInitializingBean>[] initAfter() {
        return new Class[]{
                InternalRepositoryService.class
        };

    }

    public void init() {
        unschedule();
        descriptor = centralConfig.getDescriptor().getIndexer();
        if (descriptor == null) {
            LOGGER.info("No indexer data configured. Using defaults.");
            descriptor = new IndexerDescriptor();
        }
        String cronExp = descriptor.getCronExp();
        try {
            new CronExpression(cronExp);
        } catch (ParseException e) {
            LOGGER.error(
                    "Bad indexer cron expression '" + cronExp + "' will be ignored (" +
                            e.getMessage() + ").");
            return;
        }
        JobDetail jobDetail = new JobDetail("indexerJobDetail", null, IndexerJob.class);
        //Schedule the croned indexing
        CronTriggerBean trigger = new CronTriggerBean();
        /* for testing */
        /*SimpleTriggerBean trigger = new SimpleTriggerBean();
        trigger.setRepeatCount(0);
        trigger.setRepeatInterval(1);
        trigger.setMisfireInstruction(
                SimpleTriggerBean.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT);*/
        /* for testing */
        trigger.setName(INDEXER_TRIGGER_NAME);
        trigger.setJobDetail(jobDetail);
        try {
            trigger.setCronExpression(cronExp);
        } catch (ParseException e) {
            throw new RuntimeException("Invalid cron exp '" + cronExp + "'.", e);
        }
        try {
            trigger.afterPropertiesSet();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            throw new RuntimeException("Error in scheduling the indexer job.", e);
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Indexer activated with cron expression '" + cronExp + "'.");
        }
    }

    /**
     * @param fireTime
     * @return true if backup was successful
     */
    @SuppressWarnings({"unchecked"})
    @Transactional(readOnly = true)
    public boolean index(Date fireTime) {
        List<RealRepo> realRepositories = repositoryService.getLocalAndRemoteRepositories();
        List<RealRepoDescriptor> excludeRepositories = descriptor.getExcludedRepositories();
        List<RealRepo> indexedRepos = new ArrayList<RealRepo>();
        //Skip excluded repositories
        for (RealRepo repo : realRepositories) {
            boolean excluded = false;
            for (RealRepoDescriptor excludedRepo : excludeRepositories) {
                if (excludedRepo.getKey().equals(repo.getKey())) {
                    excluded = true;
                    break;
                }
            }
            if (!excluded && repo.isLocal()) {
                indexedRepos.add(repo);
            }
        }
        //Do the indexing work
        for (RealRepo indexedRepo : indexedRepos) {
            RepoIndexerData repoIndexerData = new RepoIndexerData(indexedRepo);
            try {
                repoIndexerData.findIndex();
                repoIndexerData.createIndex(fireTime);
                IndexerManager me = InternalContextHelper.get().beanForType(IndexerManager.class);
                me.saveIndexFiles(repoIndexerData);
            } catch (Exception e) {
                //If we failed to index because of a socket timeout, issue a terse warning instead
                //of a complete stack trace
                Throwable cause =
                        ExceptionUtils.getCauseOfTypes(e, SocketTimeoutException.class);
                if (cause != null) {
                    LOGGER.warn("Indexing for repo '" + indexedRepo.getKey() + "' failed: " +
                            e.getMessage() + ".");
                } else {
                    //Just report - don't stop indexing of other repos
                    LOGGER.error("Indexing for repo '" + indexedRepo.getKey() + "' failed.", e);
                }
            }
        }
        return true;
    }

    /**
     * Unschedule any previousely set indexing job
     */
    public void unschedule() {
        try {
            scheduler.unscheduleJob(INDEXER_TRIGGER_NAME, null);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to unschedule previous indexer job.", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveIndexFiles(RepoIndexerData repoIndexerData) {
        repoIndexerData.saveIndexFiles();
    }

}