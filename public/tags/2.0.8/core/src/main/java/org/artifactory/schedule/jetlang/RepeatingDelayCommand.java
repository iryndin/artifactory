package org.artifactory.schedule.jetlang;

import org.artifactory.schedule.TaskCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.Authentication;

/**
 * @author yoavl
 */
public abstract class RepeatingDelayCommand extends TaskCallback<RepeatingDelayCommandContext>
        implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(RepeatingDelayCommand.class);

    protected RepeatingDelayCommandContext workContext;

    protected RepeatingDelayCommand(RepeatingDelayCommandContext workContext) {
        this.workContext = workContext;
    }

    RepeatingDelayCommandContext getWorkContext() {
        return workContext;
    }

    @Override
    protected String triggeringTaskTokenFromWorkContext(RepeatingDelayCommandContext workContext) {
        return workContext.getTaskToken();
    }

    @Override
    protected Authentication getAuthenticationFromWorkContext(
            RepeatingDelayCommandContext callbackContext) {
        return null;
    }

    public void run() {
        beforeExecute(null);
        try {
            onRun();
        } catch (Exception e) {
            String msg = "Timer task '" + getClass().getName() + "' failed";
            if (log.isDebugEnabled()) {
                log.debug(msg + ".", e);
            } else {
                log.warn(msg + ": " + e.getMessage());
            }
        } finally {
            afterExecute();
        }
    }

    protected abstract void onRun() throws Exception;
}