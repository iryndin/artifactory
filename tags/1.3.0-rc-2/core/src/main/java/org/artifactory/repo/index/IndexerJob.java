package org.artifactory.repo.index;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class IndexerJob extends QuartzCommand {

    @Override
    protected void onExecute(JobExecutionContext context) throws JobExecutionException {
        ArtifactoryContext artifactoryContext = ContextHelper.get();
        IndexerService indexer = artifactoryContext.beanForType(IndexerService.class);
        Date fireTime = context.getFireTime();
        indexer.index(fireTime);
    }
}