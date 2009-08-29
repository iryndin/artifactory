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
package org.artifactory.concurrent;

import org.artifactory.common.ConstantsValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author freds
 * @author yoavl
 * @date Aug 28, 2009
 */
//TODO: [by yl] Copy from TaskBase - merge
public class ConcurrentStateManager {
    private static final Logger log = LoggerFactory.getLogger(ConcurrentStateManager.class);

    private BaseState state;
    private final ReentrantLock stateSync;
    private final Condition stateChanged;

    public ConcurrentStateManager(BaseState initState) {
        this.state = initState;
        this.stateSync = new ReentrantLock();
        this.stateChanged = stateSync.newCondition();
    }

    public <V> V changeStateIn(Callable<V> toCall) {
        lockState();
        try {
            return toCall.call();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        } finally {
            unlockState();
        }
    }

    public <V> V guardedTransitionToState(BaseState newState, boolean waitForNextStep, Callable<V> callable) {
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

    public BaseState guardedWaitForNextStep() {
        long timeout = ConstantsValue.lockTimeoutSecs.getLong();
        return guardedWaitForNextStep(timeout);
    }

    public BaseState guardedWaitForNextStep(long timeout) {
        BaseState oldState = state;
        BaseState newState = oldState;
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

    public void guardedSetState(BaseState newState) {
        boolean validNewState = state.canTransitionTo(newState);
        if (!validNewState) {
            throw new IllegalArgumentException("Cannot transition from " + this.state + " to " + newState + ".");
        }
        log.trace("Changing state: {}-->{}", this.state, newState);
        state = newState;
        stateChanged.signal();
    }

    public BaseState getState() {
        return state;
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
                throw new LockingException("Locking already locked state: " +
                        this + " active lock(s) already active!");
            }
            boolean sucess = stateSync.tryLock() || stateSync.tryLock(getStateLockTimeOut(), TimeUnit.SECONDS);
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

    private static void catchInterrupt(BaseState state) {
        log.warn("Interrupted during state wait from '{}'.", state);
    }
}
