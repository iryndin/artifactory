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

import javax.annotation.Nonnull;
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

    void debug(String statusMsg, @Nonnull Logger logger);

    void status(String statusMsg, @Nonnull Logger logger);

    void status(String statusMsg, int statusCode, @Nonnull Logger logger);

    void warn(String statusMsg, @Nonnull Logger logger);

    void warn(String statusMsg, Throwable throwable, @Nonnull Logger logger);

    void error(String statusMsg, @Nonnull Logger logger);

    void error(String statusMsg, int statusCode, @Nonnull Logger logger);

    void error(String status, Throwable throwable, @Nonnull Logger logger);

    void error(String statusMsg, int statusCode, Throwable throwable, @Nonnull Logger logger);

    void setActivateLogging(boolean activateLogging);

    void reset();

    boolean isVerbose();

    File getOutputFile();

    void setOutputFile(File absoluteFile);
}
