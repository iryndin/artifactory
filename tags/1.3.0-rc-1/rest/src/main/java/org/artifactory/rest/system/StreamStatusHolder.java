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
package org.artifactory.rest.system;


import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusEntry;
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

    @Override
    protected StatusEntry addStatus(String statusMsg, int statusCode, Logger logger, boolean debug) {
        StatusEntry result = super.addStatus(statusMsg, statusCode, logger, debug);
        if (isVerbose() || !result.isDebug()) {
            String msg = result.getStatusMessage() + "\n";
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

    @Override
    protected StatusEntry addError(String statusMsg, int statusCode, Throwable throwable,
            Logger logger,
            boolean warn) {
        StatusEntry result = super.addError(statusMsg, statusCode, throwable, logger, warn);
        if (!brokenPipe) {
            try {
                PrintStream os = getResponseStream();
                os.println("" + statusCode + " : " + statusMsg);
                if (throwable != null) {
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
}
