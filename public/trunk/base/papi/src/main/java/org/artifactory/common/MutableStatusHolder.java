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

package org.artifactory.common;

import org.slf4j.Logger;

import java.io.File;

/**
 * @author Yoav Landman
 */
public interface MutableStatusHolder extends StatusHolder {
    @Override
    StatusEntry getStatusEntry();

    @Override
    String getStatusMsg();

    @Override
    boolean isError();

    @Override
    Throwable getException();

    @Override
    int getStatusCode();

    void setFastFail(boolean failFast);

    void setVerbose(boolean verbose);

    void setLastError(StatusEntry error);

    void setDebug(String statusMsg, Logger logger);

    void setStatus(String statusMsg, Logger logger);

    void setStatus(String statusMsg, int statusCode, Logger logger);

    void setError(String statusMsg, Logger logger);

    void setError(String statusMsg, int statusCode, Logger logger);

    void setError(String status, Throwable throwable, Logger logger);

    void setError(String status, int statusCode, Throwable throwable);

    void setError(String statusMsg, int statusCode, Throwable throwable, Logger logger);

    void setWarning(String statusMsg, Logger logger);

    void setWarning(String statusMsg, Throwable throwable, Logger logger);

    void setActivateLogging(boolean activateLogging);

    void reset();

    void setOutputFile(File callback);

    boolean isVerbose();
}
