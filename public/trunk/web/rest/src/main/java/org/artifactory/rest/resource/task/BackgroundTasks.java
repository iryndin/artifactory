package org.artifactory.rest.resource.task;

import com.google.common.collect.Lists;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * Simple list wrapper to return JSON string with list of tasks
 *
 * @author Yossi Shaul
 */
public class BackgroundTasks {

    private List<BackgroundTask> tasks = Lists.newArrayList();

    public BackgroundTasks(@JsonProperty("tasks") List<BackgroundTask> tasks) {
        this.tasks = tasks;
    }

    public List<BackgroundTask> getTasks() {
        return Lists.newArrayList(tasks);
    }

    public void addTasks(List<BackgroundTask> tasks) {
        this.tasks.addAll(tasks);
    }

    @Override
    public String toString() {
        return "BackgroundTasks{tasks=" + tasks + '}';
    }
}
