/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.schedule;

import org.artifactory.common.ConstantsValue;
import org.artifactory.jcr.lock.LockingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private State state;
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
    private static final int TRIES_UNTIL_COMPLETION = 100;

    @SuppressWarnings({"unchecked"})
    protected TaskBase(Class<? extends TaskCallback> callbackType) {
        state = State.VIRGIN;
        stateSync = new ReentrantLock();
        stateChanged = stateSync.newCondition();
        completed = stateSync.newCondition();
        executed = false;
        resumeBarriersCount = 0;
        this.callbackType = callbackType;
        this.token = UUID.randomUUID().toString();
    }

    void schedule() {
        lockState();
        try {
            guardedTransitionToState(State.SCHEDULED, false, new Callable<Object>() {
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
     * Stops but does not unschedule the task (can transition back to runnning state)
     *
     * @param wait
     */
    void stop(boolean wait, boolean pause, boolean cancel) {
        if (pause & cancel) {
            throw new IllegalArgumentException("Please decide wheather you wish to pause or to cancel!");
        }
        lockState();
        try {
            log.trace("Entering stop with state {} on {}", state, this);
            if (state == State.VIRGIN) {
                throw new IllegalStateException("Cannot stop a virgin task.");
            } else if (state == State.SCHEDULED) {
                //Not in execution loop
                if (cancel) {
                    guardedSetState(State.CANCELED);
                    cancelTask();
                } else {
                    //For both stop and pause
                    resumeBarriersCount++;
                    log.trace("resumeBarriersCount++ to {} after pause/stop on {}", resumeBarriersCount, this);
                    guardedSetState(State.STOPPED);
                }
            } else if (state == State.RUNNING) {
                if (pause) {
                    resumeBarriersCount++;
                    log.trace("resumeBarriersCount++ to {} after pause on {}", resumeBarriersCount, this);
                    guardedTransitionToState(State.PAUSING, wait, null);
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
                    guardedTransitionToState(State.STOPPING, wait, callback);
                }
            } else if (state == State.STOPPED || state == State.PAUSED) {
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
            if (state == State.CANCELED) {
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
            if (state == State.PAUSED || state == State.PAUSING) {
                guardedSetState(State.RUNNING);
            } else if (state == State.STOPPED || state == State.STOPPING) {
                //Nothing to do for single execution - either resume from pause or reached stopped
                //if resume by a different thread
                if (!isSingleExecution()) {
                    guardedSetState(State.SCHEDULED);
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
            if (state == State.PAUSING) {
                guardedSetState(State.PAUSED);
            }
            while (state == State.PAUSED) {
                guardedWaitForNextStep();
            }
            return state != State.RUNNING;
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
                //Wait forever (100 times more than timeout) until it finished the current execution
                int tries = TRIES_UNTIL_COMPLETION;
                while (!(completed = executed && (state == State.STOPPED || state == State.CANCELED))) {
                    boolean success = this.completed.await(ConstantsValue.lockTimeoutSecs.getLong(), TimeUnit.SECONDS);
                    if (success) {
                        completed = true;
                        break;
                    }
                    tries--;
                    if (tries <= 0) {
                        throw new LockingException("Waited for task " + this + " more than " + TRIES_UNTIL_COMPLETION +
                                " times.");
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
            if (state == State.SCHEDULED) {
                guardedSetState(State.RUNNING);
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
            if (state == State.STOPPED) {
                //Do nothing
            } else if (state == State.PAUSED || state == State.STOPPING || state == State.PAUSING) {
                guardedSetState(State.STOPPED);
            } else if (state != State.CANCELED) {
                if (isSingleExecution()) {
                    guardedSetState(State.STOPPED);
                } else if (state != State.SCHEDULED) {
                    //Could be on SCHEDULED if resumed after stopped
                    guardedSetState(State.SCHEDULED);
                }
            }
            completed.signal();
        } finally {
            unlockState();
        }
    }

    private <V> V guardedTransitionToState(State newState, boolean waitForNextStep, Callable<V> callable) {
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

    private State guardedWaitForNextStep() {
        long timeout = ConstantsValue.lockTimeoutSecs.getLong();
        return guardedWaitForNextStep(timeout);
    }

    private State guardedWaitForNextStep(long timeout) {
        State oldState = state;
        State newState = oldState;
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

    private void guardedSetState(State newState) {
        boolean validNewState = state.canTransitionTo(newState);
        if (!validNewState) {
            throw new IllegalArgumentException("Cannot transition from " + this.state + " to " + newState + ".");
        }
        log.trace("Changing state: {}-->{}", this.state, newState);
        state = newState;
        if (state == State.RUNNING) {
            executed = true;
        } else if (state == State.SCHEDULED) {
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
                throw new LockingException("Locking already locked task state: " +
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
        return ConstantsValue.lockTimeoutSecs.getLong();
    }

    private void unlockState() {
        log.trace("Unlocking {}", this);
        stateSync.unlock();
    }

    private static void catchInterrupt(State state) {
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

    public enum State {
        VIRGIN,
        SCHEDULED,
        RUNNING,
        PAUSING,
        STOPPING,
        STOPPED, //Will not start if refired by the scheduler
        PAUSED, //Blocked by executions thread (and will not start if refired by scheduler)
        CANCELED;

        public boolean canTransitionTo(State newState) {
            Set<State> states = getPossibleTransitionStates(this);
            return states.contains(newState);
        }

        @SuppressWarnings({"OverlyComplexMethod"})
        private static Set<State> getPossibleTransitionStates(State oldState) {
            HashSet<State> states = new HashSet<State>();
            switch (oldState) {
                case VIRGIN:
                    states.add(State.SCHEDULED);
                    states.add(State.CANCELED);
                    return states;
                case SCHEDULED:
                    states.add(State.RUNNING);
                    states.add(State.PAUSING);
                    states.add(State.STOPPING);
                    states.add(State.STOPPED);
                    states.add(State.CANCELED);
                    return states;
                case RUNNING:
                    states.add(State.PAUSING);
                    states.add(State.STOPPING);
                    states.add(State.STOPPED);
                    states.add(State.CANCELED);
                    states.add(State.SCHEDULED);
                    return states;
                case PAUSING:
                    states.add(State.PAUSED);
                    states.add(State.RUNNING);
                    states.add(State.STOPPING);
                    states.add(State.STOPPED);
                    states.add(State.CANCELED);
                    return states;
                case PAUSED:
                    states.add(State.RUNNING);
                    states.add(State.STOPPING);
                    states.add(State.CANCELED);
                    return states;
                case STOPPING:
                    states.add(State.CANCELED);
                case STOPPED:
                    states.add(State.STOPPED);
                    states.add(State.RUNNING);
                    states.add(State.SCHEDULED);
                    states.add(State.CANCELED);
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