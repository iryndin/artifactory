package org.artifactory.schedule;

/**
 * Represents the control unit over execution (as opposed to the actual work callback). Has a state
 * that can be controlled through the {@link TaskService}.
 * <p/>
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface Task {
    Class getType();

    String getToken();
}
