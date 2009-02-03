package org.artifactory.scheduling;

import org.apache.log4j.Logger;
import org.artifactory.spring.ArtifactoryApplicationContext;
import org.artifactory.spring.ArtifactoryContextThreadBinder;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.TimerTask;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class ArtifactoryTimerTask extends TimerTask implements ApplicationContextAware {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactoryTimerTask.class);

    private ArtifactoryApplicationContext artifactoryContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.artifactoryContext = (ArtifactoryApplicationContext) applicationContext;
    }

    public ArtifactoryApplicationContext getArtifactoryContext() {
        return artifactoryContext;
    }

    public final void run() {
        ArtifactoryContextThreadBinder.bind(artifactoryContext);
        try {
            onRun();
        } finally {
            ArtifactoryContextThreadBinder.unbind();
        }
    }

    protected abstract void onRun();
}