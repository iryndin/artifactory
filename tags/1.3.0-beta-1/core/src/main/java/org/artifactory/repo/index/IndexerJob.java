package org.artifactory.repo.index;

import org.apache.log4j.Logger;
import org.artifactory.backup.BackupJob;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.scheduling.ArtifactoryJob;
import org.artifactory.spring.ArtifactoryContext;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class IndexerJob extends ArtifactoryJob {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(BackupJob.class);

    @SuppressWarnings({"unchecked"})
    protected void onExecute(JobExecutionContext context, JcrSessionWrapper jcrSession)
            throws JobExecutionException {
        jcrSession.setReadOnly(true);
        ArtifactoryContext artifactoryContext = getArtifactoryContext();
        IndexerManager indexerManager = artifactoryContext.beanForType(IndexerManager.class);
        Date fireTime = context.getFireTime();
        indexerManager.index(fireTime);
    }
}