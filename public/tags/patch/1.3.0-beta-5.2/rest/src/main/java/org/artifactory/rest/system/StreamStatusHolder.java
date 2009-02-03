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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.artifactory.api.common.MultiStatusesHolder;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;

/**
 * User: freds Date: Aug 12, 2008 Time: 7:49:54 PM
 */
public class StreamStatusHolder extends MultiStatusesHolder {
    private static final Logger LOGGER =
            LogManager.getLogger(StreamStatusHolder.class);

    private HttpServletResponse response;
    private PrintStream out;
    private boolean brokenPipe = false;

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
                LOGGER.error("Cannot create writer stream to client " + e.getMessage(), e);
                brokenPipe = true;
            }
        }
        return out;
    }

    @Override
    protected void addStatus(String statusMsg, int statusCode, Logger logger, boolean debug) {
        super.addStatus(statusMsg, statusCode, logger, debug);
        sendToClient(statusMsg);
    }

    private void sendToClient(String statusMsg) {
        if (!brokenPipe) {
            try {
                PrintStream os = getResponseStream();
                os.print(statusMsg);
                os.flush();
            } catch (Exception e) {
                LOGGER.error("Cannot send status to client. Will stop sending them.", e);
                brokenPipe = true;
            }
        }
    }

    @Override
    protected void addError(String statusMsg, int statusCode, Throwable throwable, Logger logger,
            boolean warn) {
        super.addError(statusMsg, statusCode, throwable, logger, warn);
        if (!brokenPipe) {
            try {
                PrintStream os = getResponseStream();
                os.println("" + statusCode + " : " + statusMsg);
                if (throwable != null) {
                    throwable.printStackTrace(os);
                }
                os.flush();
            } catch (Exception e) {
                LOGGER.error("Cannot send status to client. Will stop sending them.", e);
                brokenPipe = true;
            }
        }
    }
}
