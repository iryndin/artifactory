/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

import org.artifactory.common.ConstantValues;
import org.artifactory.concurrent.LockingException;
import org.artifactory.concurrent.State;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.util.ClassUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class TaskBase implements Task {
    private static final Logger log = LoggerFactory.getLogger(TaskBase.class);

    private TaskState state;
    //TODO: [by fsi] should use StateManager
    private final ReentrantLock stateSync;
    private final Condition stateChanged;
    private final Condition completed;
    private boolean executed;
    //TODO: [by yl] This should be on callback type level (maybe configure all callbacks an an enum or general job
    //definition service)
    private boolean singleton;

    /**
     * Prevents resuming a task until all stoppers/pausers have resumed it
     */
    private int resumeBarriersCount;
    private final Class<? extends TaskCallback> callbackType;

    private final String token;

    @SuppressWarnings({"unchecked"})
    protected TaskBase(Class<? extends TaskCallback> callbackType) {
        state = TaskState.VIRGIN;
        stateSync = new ReentrantLock();
        stateChanged = stateSync.newCondition();
        completed = stateSync.newCondition();
        executed = false;
        resumeBarriersCount = 0;
        this.callbackType = callbackType;
        this.token = UUID.randomUUID().toString();
    }

    public State getInitialState() {
        return TaskState.VIRGIN;
    }

    void schedule() {
        lockState();
        try {
            guardedTransitionToState(TaskState.SCHEDULED, false, new Callable<Object>() {
                public Object call() throws Exception {
                    scheduleTask();
                    return null;
                }
            });
        } finally {
            unlockState();
        }
    }

    /**
     * Stops but does not unschedule the task (can transition back to running state)
     *
     * @param wait
     */
    void stop(boolean wait, boolean pause, boolean cancel) {
        if (pause & cancel) {
            throw new IllegalArgumentException("Please decide whether you wish to pause or to cancel!");
        }
        lockState();
        try {
            log.trace("Entering stop with state {} on {}", state, this);
            if (state == TaskState.VIRGIN) {
                throw new IllegalStateException("Cannot stop a virgin task.");
            } else if (state == TaskState.SCHEDULED) {
                //Not in execution loop
                if (cancel) {
                    guardedSetState(TaskState.CANCELED);
                    cancelTask();
                } else {
                    //For both stop and pause
                    resumeBarriersCount++;
                    log.trace("resumeBarriersCount++ to {} after pause/stop on {}", resumeBarriersCount, this);
                    guardedSetState(TaskState.STOPPED);
                }
            } else if (state == TaskState.RUNNING) {
                if (pause) {
                    resumeBarriersCount++;
                    log.trace("resumeBarriersCount++ to {} after pause on {}", resumeBarriersCount, this);
                    guardedTransitionToState(TaskState.PAUSING, wait, null);
                } else {
                    Callable<Object> callback = null;
                    if (cancel) {
                        log.trace("Canceling running task: {}.", this);
                        callback = new Callable<Object>() {
                            public Object call() throws Exception {
                                cancelTask();
                                return null;
                            }
                        };
                    } else {
                        //Stop
                        resumeBarriersCount++;
                        log.trace("resumeBarriersCount++ to {} after stop on {}", resumeBarriersCount, this);
                    }
                    guardedTransitionToState(TaskState.STOPPING, wait, callback);
                }
            } else if (state == TaskState.STOPPED || state == TaskState.PAUSED) {
                //Stop/pause
                resumeBarriersCount++;
                log.trace("resumeBarriersCount++ to {} after re-stop/re-pause on {}", resumeBarriersCount, this);
            }
        } finally {
            unlockState();
        }
    }

    boolean resume() {
        lockState();
        try {
            if (state == TaskState.CANCELED) {
                throw new IllegalStateException("Cannot resume a canceled task.");
            }
            if (resumeBarriersCount > 0) {
                resumeBarriersCount--;
            } else {
                log.warn("Skipping resume since there are no active resume barriers. " +
                        "Maybe invoked resume() more than needed.");
                return true;
            }
            log.trace("resumeBarriersCount-- to {} after resume on {}", resumeBarriersCount, this);
            if (resumeBarriersCount > 0) {
                log.debug("Cannot resume while there are still {} resume barriers.", resumeBarriersCount);
                return false;
            }
            if (state == TaskState.PAUSED || state == TaskState.PAUSING) {
                guardedSetState(TaskState.RUNNING);
            } else if (state == TaskState.STOPPED || state == TaskState.STOPPING) {
                //Nothing to do for single execution - either resume from pause or reached stopped
                //if resume by a different thread
                if (!isSingleExecution()) {
                    guardedSetState(TaskState.SCHEDULED);
                }
            }
            return true;
        } finally {
            unlockState();
        }
    }

    public Class<? extends TaskCallback> getType() {
        return callbackType;
    }

    public String getToken() {
        return token;
    }

    /**
     * Starts or schedules the task
     */
    protected abstract void scheduleTask();

    /**
     * Stops or unschedules the task
     */
    protected abstract void cancelTask();

    /**
     * Needs to be called from the execution loop of the task that wants to check if to pause or to stop
     *
     * @return
     */
    public boolean blockIfPausedAndShouldBreak() {
        lockState();
        //if running continue, if pausing transition to pause else exit
        try {
            if (state == TaskState.PAUSING) {
                guardedSetState(TaskState.PAUSED);
            }
            while (state == TaskState.PAUSED) {
                guardedWaitForNextStep();
            }
            return state != TaskState.RUNNING;
        } finally {
            unlockState();
        }
    }

    /**
     * Whether this task is non-cyclic one and is canceled after a single execution
     *
     * @return
     */
    public boolean isSingleExecution() {
        return false;
    }

    /**
     * Weather the task with this callback type should be unique on the tast service. I.e. not other tasks with the same
     * type should ever be running.
     *
     * @return True if this task should be unique
     */
    public boolean isSingleton() {
        return singleton;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    /**
     * Wait for the task to stop running
     *
     * @return
     */
    boolean waitForCompletion() {
        if (!isSingleExecution()) {
            throw new UnsupportedOperationException("Does not support waitForCompletion on cyclic tasks.");
        }
        boolean completed = false;
        lockState();
        try {
            try {
                //Wait forever (tries * lock timeout) until it finished the current execution
                int tries = ConstantValues.taskCompletionLockTimeoutRetries.getInt();
                while (!(completed = executed && (state == TaskState.STOPPED || state == TaskState.CANCELED))) {
                    boolean success = this.completed.await(ConstantValues.locksTimeoutSecs.getLong(), TimeUnit.SECONDS);
                    if (success) {
                        completed = true;
                        break;
                    }
                    tries--;
                    if (tries <= 0) {
                        throw new LockingException("Waited for task " + this + " more than " +
                                ConstantValues.taskCompletionLockTimeoutRetries.getInt() + " times.");
                    }
                }
            } catch (InterruptedException e) {
                catchInterrupt(state);
            }
        } finally {
            unlockState();
        }
        return completed;
    }

    boolean started() {
        boolean shouldExecute = false;
        lockState();
        //Check if should run
        try {
            if (state == TaskState.SCHEDULED) {
                guardedSetState(TaskState.RUNNING);
                shouldExecute = true;
            }
        } finally {
            unlockState();
        }
        return shouldExecute;
    }

    void completed() {
        lockState();
        try {
            if (state == TaskState.STOPPED) {
                //Do nothing
            } else if (state == TaskState.PAUSED || state == TaskState.STOPPING || state == TaskState.PAUSING) {
                guardedSetState(TaskState.STOPPED);
            } else if (state != TaskState.CANCELED) {
                if (isSingleExecution()) {
                    guardedSetState(TaskState.STOPPED);
                } else if (state != TaskState.SCHEDULED) {
                    //Could be on SCHEDULED if resumed after stopped
                    guardedSetState(TaskState.SCHEDULED);
                }
            }
            completed.signal();
        } finally {
            unlockState();
        }
    }

    private <V> V guardedTransitionToState(TaskState newState, boolean waitForNextStep, Callable<V> callable) {
        V result = null;
        if (state == newState) {
            return result;
        }
        if (callable != null) {
            try {
                result = callable.call();
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to switch state from '" + this.state + "' to '" + newState + "'.", e);
            }
        }
        guardedSetState(newState);
        if (waitForNextStep) {
            guardedWaitForNextStep();
        }
        return result;
    }

    private TaskState guardedWaitForNextStep() {
        long timeout = ConstantValues.locksTimeoutSecs.getLong();
        return guardedWaitForNextStep(timeout);
    }

    private TaskState guardedWaitForNextStep(long timeout) {
        TaskState oldState = state;
        TaskState newState = oldState;
        try {
            log.trace("Entering wait for next step from {} on: {}", oldState, this);
            while (state == oldState) {
                boolean success = stateChanged.await(timeout, TimeUnit.SECONDS);
                if (!success) {
                    throw new LockingException(
                            "Timeout after " + timeout + " seconds when trying to wait for next state in '" + oldState +
                                    "'.");
                }
                newState = state;
                log.trace("Exiting wait for next step from {} to {} on {}", new Object[]{oldState, newState, this});
            }
        } catch (InterruptedException e) {
            catchInterrupt(oldState);
        }
        return newState;
    }

    private void guardedSetState(TaskState newState) {
        boolean validNewState = state.canTransitionTo(newState);
        if (!validNewState) {
            throw new IllegalArgumentException("Cannot transition from " + this.state + " to " + newState + ".");
        }
        log.trace("Changing state: {}-->{}", this.state, newState);
        state = newState;
        if (state == TaskState.RUNNING) {
            executed = true;
        } else if (state == TaskState.SCHEDULED) {
            executed = false;
        }
        stateChanged.signal();
    }

    private void lockState() {
        try {
            int holdCount = stateSync.getHoldCount();
            log.trace("Thread {} trying lock (activeLocks={}) on {}",
                    new Object[]{Thread.currentThread(), holdCount, this});
            if (holdCount > 0) {
                //Clean all and throw
                while (holdCount > 0) {
                    stateSync.unlock();
                    holdCount--;
                }
                throw new LockingException("Locking an already locked task state: " +
                        this + " active lock(s) already active!");
            }
            boolean sucess = stateSync.tryLock() ||
                    stateSync.tryLock(getStateLockTimeOut(), TimeUnit.SECONDS);
            if (!sucess) {
                throw new LockingException(
                        "Could not acquire state lock in " + getStateLockTimeOut());
            }
        } catch (InterruptedException e) {
            log.warn("Interrpted while trying to lock {}.", this);
        }
    }

    private long getStateLockTimeOut() {
        return ConstantValues.locksTimeoutSecs.getLong();
    }

    private void unlockState() {
        log.trace("Unlocking {}", this);
        stateSync.unlock();
    }

    private static void catchInterrupt(TaskState state) {
        log.warn("Interrupted during state wait from '{}'.", state);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TaskBase)) {
            return false;
        }
        TaskBase base = (TaskBase) o;
        return token.equals(base.token);
    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }

    @Override
    public String toString() {
        return ClassUtils.getShortName(getType()) + "#" + token;
    }

    public enum TaskState implements State {
        VIRGIN,
        SCHEDULED,
        RUNNING,
        PAUSING,
        STOPPING,
        STOPPED, //Will not start if refired by the scheduler
        PAUSED, //Blocked by executions thread (and will not start if refired by scheduler)
        CANCELED;

        @SuppressWarnings({"SuspiciousMethodCalls"})
        public boolean canTransitionTo(State newState) {
            Set<TaskState> states = getPossibleTransitionStates(this);
            return states.contains(newState);
        }

        @SuppressWarnings({"OverlyComplexMethod"})
        private static Set<TaskState> getPossibleTransitionStates(TaskState oldState) {
            HashSet<TaskState> states = new HashSet<TaskState>();
            switch (oldState) {
                case VIRGIN:
                    states.add(TaskState.SCHEDULED);
                    states.add(TaskState.CANCELED);
                    return states;
                case SCHEDULED:
                    states.add(TaskState.RUNNING);
                    states.add(TaskState.PAUSING);
                    states.add(TaskState.STOPPING);
                    states.add(TaskState.STOPPED);
                    states.add(TaskState.CANCELED);
                    return states;
                case RUNNING:
                    states.add(TaskState.PAUSING);
                    states.add(TaskState.STOPPING);
                    states.add(TaskState.STOPPED);
                    states.add(TaskState.CANCELED);
                    states.add(TaskState.SCHEDULED);
                    return states;
                case PAUSING:
                    states.add(TaskState.PAUSED);
                    states.add(TaskState.RUNNING);
                    states.add(TaskState.STOPPING);
                    states.add(TaskState.STOPPED);
                    states.add(TaskState.CANCELED);
                    return states;
                case PAUSED:
                    states.add(TaskState.RUNNING);
                    states.add(TaskState.STOPPING);
                    states.add(TaskState.CANCELED);
                    return states;
                case STOPPING:
                    states.add(TaskState.CANCELED);
                case STOPPED:
                    states.add(TaskState.STOPPED);
                    states.add(TaskState.RUNNING);
                    states.add(TaskState.SCHEDULED);
                    states.add(TaskState.CANCELED);
                    return states;
                case CANCELED:
                    //Unscheduled
                    return states;
                default:
                    throw new IllegalArgumentException(
                            "No transitions defined for state: " + oldState);
            }
        }
    }

}