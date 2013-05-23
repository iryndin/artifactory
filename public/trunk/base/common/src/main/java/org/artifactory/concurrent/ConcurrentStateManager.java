/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.concurrent;

import org.artifactory.common.ConstantValues;
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

    private State state;
    private final StateAware stateAware;
    private final ReentrantLock stateSync;
    private final Condition stateChanged;

    public ConcurrentStateManager(StateAware stateAware) {
        this.stateAware = stateAware;
        this.state = stateAware.getInitialState();
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

    public <V> V guardedTransitionToState(State newState, boolean waitForNextStep, Callable<V> callable) {
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

    public State guardedWaitForNextStep() {
        long timeout = ConstantValues.locksTimeoutSecs.getLong();
        return guardedWaitForNextStep(timeout);
    }

    public State guardedWaitForNextStep(long timeout) {
        State oldState = state;
        State newState = oldState;
        try {
            log.trace("Entering wait for next step from {} on: {}", oldState, stateAware);
            while (state == oldState) {
                boolean success = stateChanged.await(timeout, TimeUnit.SECONDS);
                if (!success) {
                    throw new LockingException(
                            "Timeout after " + timeout + " seconds when trying to wait for next state in '" + oldState +
                                    "'.");
                }
                newState = state;
                log.trace("Exiting wait for next step from {} to {} on {}",
                        new Object[]{oldState, newState, stateAware});
            }
        } catch (InterruptedException e) {
            catchInterrupt(oldState);
        }
        return newState;
    }

    public void guardedSetState(State newState) {
        boolean validNewState = state.canTransitionTo(newState);
        if (!validNewState) {
            throw new IllegalArgumentException("Cannot transition from " + this.state + " to " + newState + ".");
        }
        log.trace("Changing state: {}-->{}", this.state, newState);
        state = newState;
        stateChanged.signal();
    }

    public State getState() {
        return state;
    }

    private void lockState() {
        try {
            int holdCount = stateSync.getHoldCount();
            log.trace("Thread {} trying lock (activeLocks={}) on {}",
                    new Object[]{Thread.currentThread(), holdCount, stateAware});
            if (holdCount > 0) {
                //Clean all and throw
                while (holdCount > 0) {
                    stateSync.unlock();
                    holdCount--;
                }
                throw new LockingException("Locking an already locked state: " +
                        stateAware + " active lock(s) already active!");
            }
            boolean success = stateSync.tryLock() || stateSync.tryLock(getStateLockTimeOut(), TimeUnit.SECONDS);
            if (!success) {
                throw new LockingException(
                        "Could not acquire state lock in " + getStateLockTimeOut());
            }
        } catch (InterruptedException e) {
            // Set the interrupted flag back
            Thread.currentThread().interrupt();
            throw new LockingException("Interrupted while trying to lock " + stateAware, e);
        }
    }

    private long getStateLockTimeOut() {
        return ConstantValues.locksTimeoutSecs.getLong();
    }

    private void unlockState() {
        log.trace("Unlocking {}", stateAware);
        stateSync.unlock();
    }

    private static void catchInterrupt(State state) {
        log.warn("Interrupted during state wait from '{}'.", state);
    }
}
