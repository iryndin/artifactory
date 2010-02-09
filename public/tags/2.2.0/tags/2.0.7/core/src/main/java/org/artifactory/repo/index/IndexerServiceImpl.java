package org.artifactory.repo.index;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.util.ExceptionUtils;
import org.quartz.CronExpression;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.CronTriggerBean;
import org.springframework.stereotype.Service;

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
public class IndexerServiceImpl implements IndexerService {
    private static final Logger log = LoggerFactory.getLogger(IndexerServiceImpl.class);

    private static final String INDEXER_TRIGGER_NAME = "indexerTrigger";

    @Autowired
    private TaskService taskService;

    @Autowired
    private CentralConfigService centralConfig;

    @Autowired
    private InternalRepositoryService repositoryService;

    private IndexerDescriptor descriptor;

    @PostConstruct
    public void register() {
        InternalContextHelper.get().addReloadableBean(IndexerService.class);
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{TaskService.class, InternalRepositoryService.class};
    }

    public void init() {
        descriptor = centralConfig.getDescriptor().getIndexer();
        if (descriptor == null) {
            log.warn("No indexer is configured. m2eclipse indexing will be disabled.");
            return;
        }
        String cronExp = descriptor.getCronExp();
        try {
            new CronExpression(cronExp);
        } catch (ParseException e) {
            log.error("Bad indexer cron expression '" + cronExp + "' will be ignored (" + e.getMessage() + ").");
            return;
        }
        JobDetail jobDetail = new JobDetail("indexerJobDetail", null, IndexerJob.class);
        //Schedule the croned indexing
        CronTriggerBean trigger = new CronTriggerBean();
        trigger.setName(INDEXER_TRIGGER_NAME);
        trigger.setJobDetail(jobDetail);
        try {
            trigger.setCronExpression(cronExp);
        } catch (ParseException e) {
            throw new RuntimeException("Invalid cron exp '" + cronExp + "'.", e);
        }
        try {
            trigger.afterPropertiesSet();
            QuartzTask task = new QuartzTask(trigger);
            taskService.startTask(task);
        } catch (Exception e) {
            throw new RuntimeException("Error in scheduling the indexer job.", e);
        }
        if (log.isInfoEnabled()) {
            log.info("Indexer activated with cron expression '" + cronExp + "'.");
        }
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        taskService.stopTasks(IndexerJob.class, false);
        init();
    }

    public void destroy() {
    }

    /**
     * @param fireTime
     */
    @SuppressWarnings({"unchecked"})
    public void index(Date fireTime) {
        List<RealRepo> realRepositories = repositoryService.getLocalAndRemoteRepositories();
        List<RealRepoDescriptor> excludeRepositories = descriptor.getExcludedRepositories();
        List<RealRepo> indexedRepos = new ArrayList<RealRepo>();
        //Skip excluded repositories and remote repositories that are currently offline
        for (RealRepo repo : realRepositories) {
            boolean excluded = false;
            for (RealRepoDescriptor excludedRepo : excludeRepositories) {
                if (excludedRepo.getKey().equals(repo.getKey())) {
                    excluded = true;
                    break;
                }
            }
            boolean offlineRemote = false;
            if (!repo.isLocal()) {
                offlineRemote = ((RemoteRepo) repo).isOffline();
            }
            if (!excluded && !offlineRemote) {
                indexedRepos.add(repo);
            }
        }
        //Do the indexing work
        for (RealRepo indexedRepo : indexedRepos) {
            //Check if we need to stop/suspend
            boolean stop = taskService.blockIfPausedAndShouldBreak();
            if (stop) {
                return;
            }
            RepoIndexerData repoIndexerData = new RepoIndexerData(indexedRepo);
            try {
                QuartzTask taskFindOrCreateIndex = new QuartzTask(FindOrCreateIndexJob.class, "FindOrCreateIndex");
                taskFindOrCreateIndex.addAttribute(RepoIndexerData.class.getName(), repoIndexerData);
                taskFindOrCreateIndex.addAttribute(Date.class.getName(), fireTime);
                taskService.startTask(taskFindOrCreateIndex);
                taskService.waitForTaskCompletion(taskFindOrCreateIndex.getToken());
                //Check again if we need to stop/suspend
                stop = taskService.blockIfPausedAndShouldBreak();
                if (stop) {
                    return;
                }
                QuartzTask saveIndexFileTask = new QuartzTask(SaveIndexFileJob.class, "SaveIndexFile");
                saveIndexFileTask.addAttribute(RepoIndexerData.class.getName(), repoIndexerData);
                taskService.startTask(saveIndexFileTask);
                // No real need to wait, but since other task are waiting for indexer completion, leaving it
                taskService.waitForTaskCompletion(saveIndexFileTask.getToken());
            } catch (Exception e) {
                //If we failed to index because of a socket timeout, issue a terse warning instead
                //of a complete stack trace
                Throwable cause = ExceptionUtils.getCauseOfTypes(e, SocketTimeoutException.class);
                if (cause != null) {
                    log.warn("Indexing for repo '" + indexedRepo.getKey() + "' failed: " + e.getMessage() + ".");
                } else {
                    //Just report - don't stop indexing of other repos
                    log.error("Indexing for repo '" + indexedRepo.getKey() + "' failed.", e);
                }
            }
        }
    }

    public static class FindOrCreateIndexJob extends QuartzCommand {
        @Override
        protected void onExecute(JobExecutionContext callbackContext) {
            try {
                RepoIndexerData repoIndexerData =
                        (RepoIndexerData) callbackContext.getMergedJobDataMap().get(RepoIndexerData.class.getName());
                Date fireTime = (Date) callbackContext.getMergedJobDataMap().get(Date.class.getName());
                IndexerService indexer = InternalContextHelper.get().beanForType(IndexerService.class);
                indexer.findOrCreateIndex(repoIndexerData, fireTime);
            } catch (Exception e) {
                log.error("Fetching index files failed.", e);
            }
        }
    }

    public static class SaveIndexFileJob extends QuartzCommand {
        @Override
        protected void onExecute(JobExecutionContext callbackContext) {
            try {
                RepoIndexerData repoIndexerData =
                        (RepoIndexerData) callbackContext.getMergedJobDataMap().get(RepoIndexerData.class.getName());
                IndexerService indexer = InternalContextHelper.get().beanForType(IndexerService.class);
                indexer.saveIndexFiles(repoIndexerData);
            } catch (Exception e) {
                log.error("Saving index files failed.", e);
            }
        }
    }

    public void saveIndexFiles(RepoIndexerData repoIndexerData) {
        log.debug("Saving index file for {}", repoIndexerData.indexedRepo.getKey());
        repoIndexerData.saveIndexFiles();
        log.debug("Saved index file for {}", repoIndexerData.indexedRepo.getKey());
    }

    public void findOrCreateIndex(RepoIndexerData repoIndexerData, Date fireTime) {
        log.debug("Find or create index files for {}", repoIndexerData.indexedRepo.getKey());
        repoIndexerData.findIndex();
        repoIndexerData.createIndex(fireTime);
        log.debug("Find or create index files for {}", repoIndexerData.indexedRepo.getKey());
    }
}