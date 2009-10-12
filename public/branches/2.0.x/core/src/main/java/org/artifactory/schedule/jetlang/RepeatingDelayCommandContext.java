package org.artifactory.schedule.jetlang;

import org.artifactory.spring.InternalArtifactoryContext;

/**
 * @author yoavl
 */
class RepeatingDelayCommandContext {
    private final InternalArtifactoryContext artifactoryContext;
    private String taskToken;

    public RepeatingDelayCommandContext(InternalArtifactoryContext artifactoryContext) {
        this.artifactoryContext = artifactoryContext;
    }

    public String getTaskToken() {
        return taskToken;
    }

    public InternalArtifactoryContext getArtifactoryContext() {
        return artifactoryContext;
    }

    void setTaskToken(String taskToken) {
        this.taskToken = taskToken;
    }
}