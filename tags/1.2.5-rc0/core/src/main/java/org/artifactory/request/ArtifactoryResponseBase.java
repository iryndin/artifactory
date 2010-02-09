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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class ArtifactoryResponseBase implements ArtifactoryResponse {

    private Success success = Success.unset;

    public void sendStream(InputStream is) throws IOException {
        OutputStream os = getOutputStream();
        if (success.equals(Success.unset)) {
            success = Success.success;
        }
        try {
            IOUtils.copy(is, os);
            sendOk();
        } catch (Exception e) {
            sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }
    }

    public void sendFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        sendStream(is);
    }

    public void sendError(int statusCode) throws IOException {
        success = Success.failure;
        sendErrorInternal(statusCode);
    }

    public boolean isSuccessful() {
        return success.equals(Success.success);
    }

    protected abstract void sendErrorInternal(int code) throws IOException;
}