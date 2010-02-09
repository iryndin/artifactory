package org.artifactory.scheduling;

import org.apache.log4j.Logger;
import org.artifactory.spring.ArtifactoryContext;
import org.quartz.Job;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class ArtifactoryJob implements Job {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactoryJob.class);

    protected ArtifactoryContext getArtifactoryContext() {
        return ArtifactorySchedulerFactoryBean.getSingleton();
    }
}
