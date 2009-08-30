package org.artifactory.schedule;

import org.artifactory.concurrent.StateAware;

/**
 * Represents the control unit over execution (as opposed to the actual work callback). Has a state that can be
 * controlled through the {@link TaskService}.
 * <p/>
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface Task extends StateAware {
    Class getType();

    String getToken();
}
