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
package org.artifactory.request;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

public interface ArtifactoryResponse {
    void setException(Exception exception);

    public static enum Success {
        unset, success, failure
    }

    void setLastModified(long lastModified);

    void setContentLength(int length);

    void setContentType(String contentType);

    public OutputStream getOutputStream() throws IOException;

    PrintWriter getWriter() throws IOException;

    void sendError(int statusCode) throws IOException;

    void sendStream(InputStream is) throws IOException;

    void sendFile(File targetFile) throws IOException;

    void sendOk();

    void setStatus(int status);

    void setHeader(String header, String value);

    boolean isCommitted();

    boolean isSuccessful();

    void flush();

    Exception getException();

}