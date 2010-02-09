package org.artifactory.repo.index;

import org.apache.log4j.Logger;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.config.CentralConfig;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.index.config.Indexer;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.spring.ArtifactoryContext;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.CronTriggerBean;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class IndexerManager implements ApplicationContextAware, InitializingBean {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(IndexerManager.class);

    private static final String INDEXER_TRIGGER_NAME = "indexerTrigger";

    private ArtifactoryContext context;
    private Scheduler scheduler;
    private Indexer indexer;

    public IndexerManager(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = (ArtifactoryContext) context;
    }

    public void init() {
        //Unschedule any previousely set indexing job
        try {
            scheduler.unscheduleJob(INDEXER_TRIGGER_NAME, null);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to unschedule previous indexer job.", e);
        }
        CentralConfig cc = context.getCentralConfig();
        indexer = cc.getIndexer();
        if (indexer == null) {
            LOGGER.info("No indexer data configured. Using defaults.");
            indexer = new Indexer();
        }
        String cronExp = indexer.getCronExp();
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

    public void afterPropertiesSet() throws Exception {
        init();
    }

    /**
     * @param fireTime
     * @return true if backup was successful
     */
    public boolean index(Date fireTime) {
        VirtualRepo virtualRepo = context.getCentralConfig().getGlobalVirtualRepo();
        List<RealRepo> realRepositories = virtualRepo.getLocalAndRemoteRepositories();
        List<RealRepo> excludeRepositories = indexer.getExcludedRepositories();
        List<RealRepo> indexedRepos = new ArrayList<RealRepo>();
        //Skip excluded repositories
        for (RealRepo repo : realRepositories) {
            if (!excludeRepositories.contains(repo) && repo.isLocal()) {
                indexedRepos.add(repo);
            }
        }
        //Do the indexing work
        for (RealRepo indexedRepo : indexedRepos) {
            boolean indexed = false;
            LocalRepo localRepo;
            ResourceStreamHandle indexHandle = null;
            ResourceStreamHandle propertiesHandle = null;
            //For remote repositories, try to download the remote cache, if failes index locally
            if (!indexedRepo.isLocal()) {
                RemoteRepo remoteRepo = (RemoteRepo) indexedRepo;
                localRepo = remoteRepo.getLocalCacheRepo();
                String indexPath = ".index/" + MavenUtils.NEXUS_INDEX_ZIP;
                String propertiesPath = ".index/" + MavenUtils.NEXUS_INDEX_PROPERTIES;
                try {
                    indexHandle = remoteRepo.retrieveResource(indexPath);
                    propertiesHandle = remoteRepo.retrieveResource(propertiesPath);
                    indexed = true;
                } catch (IOException e) {
                    LOGGER.warn("Could not retrieve remote nexus index '" + indexPath +
                            "' for repo '" + indexedRepo + "'.");
                }
            } else {
                localRepo = (LocalRepo) indexedRepo;
            }
            if (!indexed) {
                RepoIndexer repoIndexer = new RepoIndexer(localRepo);
                try {
                    indexHandle = repoIndexer.index(fireTime);
                    propertiesHandle = repoIndexer.getProperties();
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Failed to index repository '" + indexedRepo + "'.", e);
                }
            }
            //Create the index dir
            JcrFolder targetIndexDir = new JcrFolder(localRepo.getFolder(), ".index");
            targetIndexDir.mkdirs(false);
            //Move away the old index file
            JcrFile oldIndexFile =
                    new JcrFile(targetIndexDir, MavenUtils.NEXUS_INDEX_ZIP);
            boolean oldIndexFileExists = oldIndexFile.exists();
            if (oldIndexFileExists) {
                oldIndexFile.renameTo(new JcrFile(targetIndexDir.getAbsolutePath(),
                        MavenUtils.NEXUS_INDEX_ZIP + ".old"));
            }
            //Move away the old properties file
            JcrFile oldPropertiesFile =
                    new JcrFile(targetIndexDir,
                            MavenUtils.NEXUS_INDEX_PROPERTIES);
            boolean oldPropertiesFileExists = oldPropertiesFile.exists();
            if (oldPropertiesFileExists) {
                oldPropertiesFile.renameTo(new JcrFile(targetIndexDir.getAbsolutePath(),
                        MavenUtils.NEXUS_INDEX_PROPERTIES + ".old"));
            }
            //Create the new jcr files for index and properties
            InputStream indexInputStream = indexHandle.getInputStream();
            InputStream propertiesInputStream = propertiesHandle.getInputStream();
            try {
                JcrFile indexFile = JcrFile.create(
                        targetIndexDir, MavenUtils.NEXUS_INDEX_ZIP,
                        indexInputStream);
                JcrFile propertiesFile = JcrFile.create(
                        targetIndexDir, MavenUtils.NEXUS_INDEX_PROPERTIES,
                        propertiesInputStream);
                LOGGER.info(
                        "Successfully saved indexed file '" + indexFile.getAbsolutePath() +
                                "' and index info '" + propertiesFile.getAbsolutePath() + "'.");
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to save index file for repo '" + localRepo + "'.", e);
            } finally {
                indexHandle.close();
                propertiesHandle.close();
            }
            //Delete old index and properties
            if (oldIndexFileExists) {
                oldIndexFile.delete(false);
            }
            if (oldPropertiesFileExists) {
                oldPropertiesFile.delete(false);
            }
        }
        return true;
    }

}