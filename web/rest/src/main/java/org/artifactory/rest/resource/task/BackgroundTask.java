package org.artifactory.rest.resource.task;

import org.artifactory.rest.common.util.RestUtils;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * Data object to hold background tasks data. Translates into JSON string.
 *
 * @author Yossi Shaul
 */
public class BackgroundTask {

    private String id;
    private String type;
    private String state;
    private String description;
    /**
     * Start time in ISO8601 format
     */
    private String started;
    private String nodeId;

    @JsonCreator
    private BackgroundTask() {
    }

    /**
     * Time the task has started
     */
    public BackgroundTask(String id, String type, String state, String description, long started) {
        this.id = id;
        this.type = type;
        this.state = state;
        this.description = description;
        if (started > 0) {
            this.started = RestUtils.toIsoDateString(started);
        }
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getState() {
        return state;
    }

    public String getDescription() {
        return description;
    }

    public String getStarted() {
        return started;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    @JsonIgnore
    public long getStartedMillis() {
        return started == null ? 0 : RestUtils.fromIsoDateString(started);
    }

    @Override
    public String toString() {
        return "BackgroundTask{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", state='" + state + '\'' +
                ", started='" + started + '\'' +
                ", nodeId='" + nodeId + '\'' +
                '}';
    }
}
