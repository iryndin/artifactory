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

package org.artifactory.request;

import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.webapp.servlet.TraceLoggingResponse;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

/**
 * Manages the request tracing context
 *
 * @author Noam Y. Tenne
 */
public abstract class RequestTraceLogger {
    private static final Logger log = LoggerFactory.getLogger(RequestTraceLogger.class);

    private static ThreadLocal<Context> contextThreadLocal = new ThreadLocal<Context>();

    private RequestTraceLogger() {
    }

    /**
     * Creates a new request trace context if either the logger or the trace request is enabled
     *
     * @param methodName          HTTP method name
     * @param username            Authenticated user name
     * @param requestPath         Request repo path id
     * @param artifactoryResponse Original response
     */
    public static void startNewContext(String methodName, String username, String requestPath,
            ArtifactoryResponse artifactoryResponse) {
        boolean traceLoggingResponse = artifactoryResponse instanceof TraceLoggingResponse;
        if (log.isDebugEnabled() || traceLoggingResponse) {
            contextThreadLocal.set(new Context(UUID.randomUUID().toString().substring(0, 8), methodName.toUpperCase(),
                    username, requestPath, traceLoggingResponse ? ((TraceLoggingResponse) artifactoryResponse) : null));
        }
    }

    /**
     * Destroys the request trace context and writes the log to the response if enabled
     */
    public static void destroyContext() throws IOException {
        try {
            Context context = contextThreadLocal.get();
            if ((context != null) && context.tracingResponse) {
                context.response.sendResponse(context.id, context.methodName, context.username, context.requestPath);
            }
        } finally {
            contextThreadLocal.remove();
        }
    }

    /**
     * Logs the request status to logback and to the tracing response (if enabled)
     *
     * @param format a {@link String#format(java.lang.String, java.lang.Object...)} compatible string
     * @param params Format parameters
     */
    public static void log(String format, Object... params) {
        Context context = contextThreadLocal.get();
        if (context != null) {
            String formattedMessage = String.format(format, params);
            if (log.isDebugEnabled()) {
                log.debug(context.logSig + " " + formattedMessage);
            }
            if (context.tracingResponse) {
                context.response.log(ISODateTimeFormat.dateTime().print(System.currentTimeMillis()) + " " +
                        formattedMessage);
            }
        }
    }

    private static class Context {

        private final String id;
        private final String methodName;
        private final String username;
        private final String requestPath;
        private final boolean tracingResponse;
        private final TraceLoggingResponse response;

        private final String logSig;

        private Context(String id, String methodName, String username, String requestPath,
                TraceLoggingResponse response) {
            this.id = id;
            this.methodName = methodName;
            this.username = username;
            this.requestPath = requestPath;
            tracingResponse = (response != null);
            this.response = response;
            logSig = id + " " + methodName + " " + username + " " + requestPath;
        }
    }
}
