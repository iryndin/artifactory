package org.artifactory.repo.index;

import org.apache.log4j.Logger;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.backup.BackupJob;
import org.artifactory.schedule.ArtifactoryJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class IndexerJob extends ArtifactoryJob {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(BackupJob.class);

    @Override
    protected void onExecute(JobExecutionContext context) throws JobExecutionException {
        ArtifactoryContext artifactoryContext = ContextHelper.get();
        IndexerManager indexerManager = artifactoryContext.beanForType(IndexerManager.class);
        Date fireTime = context.getFireTime();
        indexerManager.index(fireTime);
    }
}