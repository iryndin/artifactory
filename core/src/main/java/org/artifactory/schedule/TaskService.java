/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.schedule;

import org.artifactory.spring.ReloadableBean;

/**
 * @author yoavl
 */
public interface TaskService extends ReloadableBean {

    /**
     * Starts a task and returns its token
     *
     * @param task
     * @return the token of this task
     */
    String startTask(TaskBase task);

    /**
     * Cancels and stops the task
     *
     * @param token The task token
     * @param wait  Whether to return immediately or wait for the tesk to be stopped
     * @return true if the task is actually stopped, false if already stopped
     */
    void stopTask(String token, boolean wait);

    /**
     * Pause a task
     *
     * @param token
     * @param wait
     */
    void pauseTask(String token, boolean wait);

    /**
     * If the task has been canceled (unscheduled) returns true to signal the caller that it needs to break from the
     * task's execution loop. If the task has been paused will simply pause the caller (and thus, the caller's execution
     * loop).
     *
     * @return true if the caller needs break from the task's callback execution loop
     */
    boolean pauseOrBreak();

    /**
     * Cancels and stops all active tasks of the specified type
     *
     * @param callbackType
     * @param wait
     * @return true if tasks were stops, false if all already stopped
     */
    void stopTasks(Class<? extends TaskCallback> callbackType, boolean wait);

    /**
     * Pauses all tasks of a certain type
     *
     * @param callbackType
     * @param wait
     * @return true if actually paused, false if already paused
     */
    void pauseTasks(Class<? extends TaskCallback> callbackType, boolean wait);

    /**
     * Resume a paused task
     *
     * @param token
     * @return true if managed to resume, false if there are additional stop/pause holders
     */
    boolean resumeTask(String token);

    /**
     * Resume all paused task of a certain type
     *
     * @param callbackType
     */
    void resumeTasks(Class<? extends TaskCallback> callbackType);

    /**
     * Cancels (unschedules) a task
     *
     * @param wait
     */
    void cancelTask(String token, boolean wait);

    /**
     * Cancels all tasks of a certain type
     *
     * @param callbackType
     */
    void cancelTasks(Class<? extends TaskCallback> callbackType, boolean wait);

    /**
     * Cancels all tasks
     */
    void cancelAllTasks(boolean wait);

    /**
     * Awaits task execution to be finished
     *
     * @param token
     * @return true if execution completed, otherwise false if completed outside the execution loop
     */
    boolean waitForTaskCompletion(String token);

    /**
     * Returns the actual task object.
     * <p/>
     * THIS SHOULD BE CALLED BY PRIVATE API ONLY!
     * <p/>
     * Only exists since spring jdk-proxies with which the internal api works are interfaces.
     *
     * @param token
     */
    TaskBase getInternalActiveTask(String token);

    /**
     * @param callbackType Type of callback to check if exists
     * @return True if a task with the callback type already exists
     */
    boolean hasTaskOfType(Class<? extends TaskCallback> callbackType);

    TaskBase getSingletonTaskTaskOfType(Class<? extends TaskCallback> callbackType);

    void pauseTask(String token, long timeToPause);
}