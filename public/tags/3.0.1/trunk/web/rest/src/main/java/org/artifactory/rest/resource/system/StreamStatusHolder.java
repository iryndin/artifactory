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

package org.artifactory.rest.resource.system;


import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.common.StatusEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: freds Date: Aug 12, 2008 Time: 7:49:54 PM
 */
public class StreamStatusHolder extends MultiStatusHolder {
    private static final Logger log = LoggerFactory.getLogger(StreamStatusHolder.class);

    private HttpServletResponse response;
    private PrintStream out;
    private boolean brokenPipe = false;
    private AtomicInteger doingDots = new AtomicInteger(0);

    public StreamStatusHolder(HttpServletResponse response) {
        this.response = response;
        setActivateLogging(true);
    }

    @Override
    protected StatusEntry addStatus(String statusMsg, int statusCode, Logger logger, boolean debug) {
        StatusEntry result = super.addStatus(statusMsg, statusCode, logger, debug);
        if (isVerbose() || !result.isDebug()) {
            String msg = result.getMessage() + "\n";
            int dots = doingDots.getAndSet(0);
            if (dots > 0) {
                msg = "\n" + msg;
            }
            sendToClient(msg);
        } else {
            int dots = doingDots.incrementAndGet();
            if (dots == 80) {
                doingDots.getAndAdd(-80);
                sendToClient("\n");
            } else {
                sendToClient(".");
            }
        }
        return result;
    }

    @Override
    protected StatusEntry addError(String statusMsg, int statusCode, Throwable throwable,
            Logger logger,
            boolean warn) {
        StatusEntry result = super.addError(statusMsg, statusCode, throwable, logger, warn);
        if (!brokenPipe) {
            try {
                PrintStream os = getResponseStream();
                os.println("\n" + statusCode + " : " + statusMsg);
                if ((throwable != null) && isVerbose()) {
                    throwable.printStackTrace(os);
                }
                os.flush();
            } catch (Exception e) {
                log.error("Cannot send status to client. Will stop sending them.", e);
                brokenPipe = true;
            }
        }
        return result;
    }

    private void sendToClient(String statusMsg) {
        if (!brokenPipe) {
            try {
                PrintStream os = getResponseStream();
                os.print(statusMsg);
                os.flush();
            } catch (Exception e) {
                log.error("Cannot send status to client. Will stop sending them.", e);
                brokenPipe = true;
            }
        }
    }

    private PrintStream getResponseStream() {
        if (out == null) {
            try {
                response.setContentType("text/plain;charset=utf-8");
                response.setStatus(200);
                out = new PrintStream(response.getOutputStream());
            } catch (IOException e) {
                log.error("Cannot create writer stream to client " + e.getMessage(), e);
                brokenPipe = true;
            }
        }
        return out;
    }
}
