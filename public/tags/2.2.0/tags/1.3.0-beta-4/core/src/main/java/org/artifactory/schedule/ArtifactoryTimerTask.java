package org.artifactory.schedule;

import org.apache.log4j.Logger;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.spring.InternalArtifactoryContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.TimerTask;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class ArtifactoryTimerTask extends TimerTask {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactoryTimerTask.class);

    protected InternalArtifactoryContext context;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.context = (InternalArtifactoryContext) applicationContext;
    }

    @Override
    public void run() {
        if (context == null || !context.isReady()) {
            LOGGER.info("Gave up timer trigger because initialization did not complete yet.");
            return;
        }
        ArtifactoryContextThreadBinder.bind(context);
        try {
            onRun();
        } catch (Exception e) {
            String msg = "Timer task '" + getClass().getName() + "' failed";
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(msg + ".", e);
            } else {
                LOGGER.warn(msg + ": " + e.getMessage());
            }
        } finally {
            ArtifactoryContextThreadBinder.unbind();
        }
    }

    protected abstract void onRun() throws Exception;
}