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

import javax.annotation.Nonnull;
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

    protected boolean activateLogging;
    // the latest status
    private StatusEntry statusEntry;
    protected File outputFile;
    private StatusEntry lastError = null;
    protected boolean fastFail = false;
    protected boolean verbose = false;

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

    public void setLastError(StatusEntry error) {
        this.lastError = error;
    }

    @Override
    public final void debug(String statusMsg, @Nonnull Logger logger) {
        logEntryAndAddEntry(new StatusEntry(CODE_OK, StatusEntryLevel.DEBUG, statusMsg, null), logger);
    }

    public final void setDebug(String statusMsg, int statusCode, @Nonnull Logger logger) {
        logEntryAndAddEntry(new StatusEntry(statusCode, StatusEntryLevel.DEBUG, statusMsg, null), logger);
    }

    @Override
    public final void status(String statusMsg, @Nonnull Logger logger) {
        status(statusMsg, CODE_OK, logger);
    }

    @Override
    public final void status(String statusMsg, int statusCode, @Nonnull Logger logger) {
        logEntryAndAddEntry(new StatusEntry(statusCode, statusMsg), logger);
    }

    @Override
    public void error(String status, Throwable throwable, @Nonnull Logger logger) {
        error(status, CODE_INTERNAL_ERROR, throwable, logger);
    }

    @Override
    public void error(String statusMsg, @Nonnull Logger logger) {
        error(statusMsg, CODE_INTERNAL_ERROR, null, logger);
    }

    @Override
    public void error(String statusMsg, int statusCode, @Nonnull Logger logger) {
        error(statusMsg, statusCode, null, logger);
    }

    @Override
    public void error(String statusMsg, int statusCode, Throwable throwable, @Nonnull Logger logger) {
        addError(new StatusEntry(statusCode, StatusEntryLevel.ERROR, statusMsg, throwable), logger);
    }


    @Override
    public void warn(String statusMsg, Throwable throwable, @Nonnull Logger logger) {
        addError(new StatusEntry(CODE_INTERNAL_ERROR, StatusEntryLevel.WARNING, statusMsg, throwable), logger);
    }

    @Override
    public void warn(String statusMsg, @Nonnull Logger logger) {
        addError(new StatusEntry(CODE_INTERNAL_ERROR, StatusEntryLevel.WARNING, statusMsg, null), logger);
    }

    protected StatusEntry addError(@Nonnull StatusEntry errorEntry, @Nonnull Logger logger) {
        if (isActivateLogging()) {
            logEntry(errorEntry, logger);
        }
        addStatusEntry(errorEntry);
        if (!errorEntry.isWarning() && isFastFail()) {
            Throwable throwable = errorEntry.getException();
            if (throwable != null) {
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                } else if (throwable instanceof Error) {
                    throw (Error) throwable;
                } else {
                    throw new RuntimeException("Fast fail exception: " + errorEntry.getMessage(), throwable);
                }
            } else {
                throw new RuntimeException("Fast fail exception: " + errorEntry.getMessage());
            }
        }
        return errorEntry;
    }

    protected void logEntry(@Nonnull StatusEntry entry, @Nonnull Logger logger) {
        /**
         * If an external logger is given, it shall be the active one; unless verbose output is requested, then we need
         * to use that status holder logger for the debug level
         */
        if (!isVerbose()) {
            doLogEntry(entry, logger);
        } else {
            doLogEntry(entry, log);
        }
    }

    protected void doLogEntry(@Nonnull StatusEntry entry, @Nonnull Logger logger) {
        String statusMessage = entry.getMessage();
        Throwable throwable = entry.getException();
        if (!isVerbose() && throwable != null) {
            //Update the status message for when there's an exception message to append
            statusMessage += ": " + (StringUtils.isNotBlank(throwable.getMessage()) ? throwable.getMessage() :
                    throwable.getClass().getSimpleName());
        }
        if (entry.isWarning() && logger.isWarnEnabled()) {
            if (isVerbose()) {
                logger.warn(statusMessage, throwable);
            } else {
                logger.warn(statusMessage);
            }
        } else if (entry.isError() && logger.isErrorEnabled()) {
            if (isVerbose()) {
                logger.error(statusMessage, throwable);
            } else {
                logger.error(statusMessage);
            }
        } else if (entry.isDebug() && logger.isDebugEnabled()) {
            logger.debug(statusMessage);
        } else if (entry.isInfo() && logger.isInfoEnabled()) {
            logger.info(statusMessage);
        }
    }

    @Override
    public String getStatusMsg() {
        if (lastError != null) {
            return lastError.getMessage();
        }
        return statusEntry.getMessage();
    }

    @Override
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

    protected void logEntryAndAddEntry(@Nonnull StatusEntry entry, @Nonnull Logger logger) {
        addStatusEntry(entry);
        logEntry(entry, logger);
    }

    protected void addStatusEntry(StatusEntry entry) {
        statusEntry = entry;
        if (entry.isError() && StatusEntryLevel.ERROR == entry.getLevel()) {
            lastError = entry;
        }
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
        outputFile = null;
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
