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

package org.artifactory.api.common;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang.StringUtils;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.common.StatusEntry;
import org.artifactory.common.StatusEntryLevel;
import org.artifactory.exception.CancelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * NOTE: WHEN CHANGING THE NAME OR THE PACKAGE OF THIS CLASS, MAKE SURE TO UPDATE TEST AND PRODUCTION LOGBACK
 * CONFIGURATION FILES WITH THE CHANGES AND CREATE A CONVERTER IF NEEDED. SOME APPENDERS DEPEND ON THIS.
 *
 * @author Yoav Landman
 */
@XStreamAlias("status")
public class BasicStatusHolder implements MutableStatusHolder {
    private static final Logger log = LoggerFactory.getLogger(BasicStatusHolder.class);

    protected static final String MSG_IDLE = "Idle.";
    public static final int CODE_OK = 200;
    public static final int CODE_INTERNAL_ERROR = 500;

    private boolean activateLogging;
    // the latest status
    private StatusEntry statusEntry;
    private File outputFile;
    private StatusEntry lastError = null;
    private boolean fastFail = false;
    private boolean verbose = false;

    public BasicStatusHolder() {
        statusEntry = new StatusEntry(CODE_OK, StatusEntryLevel.DEBUG, MSG_IDLE, null);
        activateLogging = true;
    }

    @Override
    public void setFastFail(boolean fastFail) {
        this.fastFail = fastFail;
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public boolean isFastFail() {
        return fastFail;
    }

    @Override
    public boolean isVerbose() {
        return verbose;
    }

    @Override
    public StatusEntry getStatusEntry() {
        return statusEntry;
    }

    @Override
    public StatusEntry getLastError() {
        return lastError;
    }

    @Override
    public void setLastError(StatusEntry error) {
        this.lastError = error;
    }

    @Override
    public final void setDebug(String statusMsg, Logger logger) {
        addStatus(statusMsg, CODE_OK, logger, true);
    }

    public final void setDebug(String statusMsg, int statusCode, Logger logger) {
        addStatus(statusMsg, statusCode, logger, true);
    }

    @Override
    public final void setStatus(String statusMsg, Logger logger) {
        setStatus(statusMsg, CODE_OK, logger);
    }

    @Override
    public final void setStatus(String statusMsg, int statusCode, Logger logger) {
        addStatus(statusMsg, statusCode, logger, false);
    }

    protected StatusEntry addStatus(String statusMsg, int statusCode, Logger logger, boolean debug) {
        StatusEntry result;
        if (debug) {
            result = new StatusEntry(statusCode, StatusEntryLevel.DEBUG, statusMsg, null);
        } else {
            result = new StatusEntry(statusCode, statusMsg);
        }
        if (activateLogging) {
            logEntry(result, logger);
        }
        statusEntry = result;
        return result;
    }

    @Override
    public void setError(String statusMsg, Logger logger) {
        setError(statusMsg, CODE_INTERNAL_ERROR, null, logger);
    }

    @Override
    public void setError(String statusMsg, int statusCode, Logger logger) {
        setError(statusMsg, statusCode, null, logger);
    }

    @Override
    public void setError(String status, Throwable throwable, Logger logger) {
        setError(status, CODE_INTERNAL_ERROR, throwable, logger);
    }

    @Override
    public void setError(String status, int statusCode, Throwable throwable) {
        setError(status, statusCode, throwable, null);
    }

    @Override
    public void setError(String statusMsg, int statusCode, Throwable throwable, Logger logger) {
        addError(statusMsg, statusCode, throwable, logger, false);
    }

    @Override
    public void setWarning(String statusMsg, Logger logger) {
        addError(statusMsg, CODE_INTERNAL_ERROR, null, logger, true);
    }

    @Override
    public void setWarning(String statusMsg, Throwable throwable, Logger logger) {
        addError(statusMsg, CODE_INTERNAL_ERROR, throwable, logger, true);
    }

    protected StatusEntry addError(String statusMsg, int statusCode, Throwable throwable, Logger logger, boolean warn) {
        StatusEntry result;
        if (warn) {
            result = new StatusEntry(statusCode, StatusEntryLevel.WARNING, statusMsg, throwable);
        } else {
            result = new StatusEntry(statusCode, StatusEntryLevel.ERROR, statusMsg, throwable);
            lastError = result;
        }
        if (isActivateLogging()) {
            logEntry(result, logger);
        }
        statusEntry = result;
        if (!warn && isFastFail()) {
            if (throwable != null) {
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                } else if (throwable instanceof Error) {
                    throw (Error) throwable;
                } else {
                    throw new RuntimeException("Fast fail exception: " + statusEntry.getMessage(), throwable);
                }
            } else {
                throw new RuntimeException("Fast fail exception: " + statusEntry.getMessage());
            }
        }
        return result;
    }

    protected void logEntry(StatusEntry entry, Logger logger) {
        Logger activeLogger;

        /**
         * If an external logger is given, it shall be the active one; unless verbose output is requested, then we need
         * to use that status holder logger for the debug level
         */
        if ((logger != null) && !isVerbose()) {
            activeLogger = logger;
        } else {
            activeLogger = log;
        }
        String statusMessage = entry.getMessage();
        Throwable throwable = entry.getException();
        if (!isVerbose() && throwable != null) {
            //Update the status message for when there's an exception message to append
            statusMessage += ": " + (StringUtils.isNotBlank(throwable.getMessage()) ? throwable.getMessage() :
                    throwable.getClass().getSimpleName());
        }
        if (entry.isWarning() && activeLogger.isWarnEnabled()) {
            if (isVerbose()) {
                activeLogger.warn(statusMessage, throwable);
            } else {
                activeLogger.warn(statusMessage);
            }
        } else if (entry.isError() && activeLogger.isErrorEnabled()) {
            if (isVerbose()) {
                activeLogger.error(statusMessage, throwable);
            } else {
                activeLogger.error(statusMessage);
            }
        } else if (entry.isDebug() && activeLogger.isDebugEnabled()) {
            activeLogger.debug(statusMessage);
        } else if (entry.isInfo() && activeLogger.isInfoEnabled()) {
            activeLogger.info(statusMessage);
        }
    }

    @Override
    public String getStatusMsg() {
        if (lastError != null) {
            return lastError.getMessage();
        }
        return statusEntry.getMessage();
    }

    public File getOutputFile() {
        return outputFile;
    }

    @Override
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public boolean isError() {
        return lastError != null;
    }

    @Override
    public CancelException getCancelException() {
        return getCancelException(null);
    }

    @Override
    public CancelException getCancelException(StatusEntry previousToLastError) {
        if (lastError != null && !lastError.equals(previousToLastError)) {
            //We have a new error check if it is a cancellation one
            Throwable cause = lastError.getException();
            if (cause != null && cause instanceof CancelException) {
                return (CancelException) cause;
            }
        }
        return null;
    }

    @Override
    public Throwable getException() {
        if (lastError != null) {
            return lastError.getException();
        }
        return statusEntry.getException();
    }

    @Override
    public int getStatusCode() {
        if (lastError != null) {
            return lastError.getStatusCode();
        }
        return statusEntry.getStatusCode();
    }

    /**
     * @return True if the status holder prints the messages to the logger.
     */
    @Override
    public boolean isActivateLogging() {
        return activateLogging;
    }

    /**
     * If set to false the status holder will not print the messages to the logger. It will only keep the statuses.
     *
     * @param activateLogging Set to fasle to disable logging
     */
    @Override
    public void setActivateLogging(boolean activateLogging) {
        this.activateLogging = activateLogging;
    }

    @Override
    public void reset() {
        lastError = null;
        statusEntry = new StatusEntry(CODE_OK, StatusEntryLevel.DEBUG, MSG_IDLE, null);
        activateLogging = true;
    }

    @Override
    public String toString() {
        return "StatusHolder{" +
                "activateLogging=" + activateLogging +
                ", statusMessage=" + statusEntry +
                ", callback=" + outputFile +
                '}';
    }
}
