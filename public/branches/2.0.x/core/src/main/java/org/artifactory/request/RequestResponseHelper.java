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
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.resource.RepoResource;
import org.artifactory.security.AccessLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public final class RequestResponseHelper {
    private static final Logger log = LoggerFactory.getLogger(RequestResponseHelper.class);

    public static void sendBodyResponse(ArtifactoryResponse response, RepoResource res, ResourceStreamHandle handle)
            throws IOException {
        try {
            updateResponseFromRepoResource(response, res);
            AccessLogger.downloaded(res.getRepoPath());
            InputStream inputStream = handle.getInputStream();
            response.sendStream(inputStream);
        } finally {
            handle.close();
        }
    }

    public static void sendBodyResponse(ArtifactoryResponse response, RepoPath repoPath, String content)
            throws IOException {
        if (content == null) {
            RuntimeException exception = new RuntimeException("Cannot send null response");
            response.sendInternalError(exception, log);
            throw exception;
        }
        byte[] bytes = content.getBytes("utf-8");
        InputStream is = new ByteArrayInputStream(bytes);
        try {
            String path = repoPath.getPath();
            String mimeType = NamingUtils.getMimeTypeByPathAsString(path);
            response.setContentType(mimeType);
            response.setContentLength(bytes.length);
            response.setLastModified(System.currentTimeMillis());
            AccessLogger.downloaded(repoPath);
            response.sendStream(is);
        } finally {
            is.close();
        }
    }

    public static void sendHeadResponse(ArtifactoryResponse response, RepoResource res) {
        if (log.isDebugEnabled()) {
            log.debug(res.getRepoPath() + ": Sending HEAD meta-information");
        }
        updateResponseFromRepoResource(response, res);
        response.sendOk();
    }

    public static void sendNotModifiedResponse(
            ArtifactoryResponse response, RepoResource res) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug(res.toString() + ": Sending NOT-MODIFIED response");
        }
        updateResponseFromRepoResource(response, res);
        response.sendError(HttpStatus.SC_NOT_MODIFIED, null, log);
    }

    private static void updateResponseFromRepoResource(ArtifactoryResponse response, RepoResource res) {
        String mimeType = res.getMimeType();
        response.setContentType(mimeType);
        response.setContentLength((int) res.getInfo().getSize());
        response.setLastModified(res.getInfo().getLastModified());
    }
}
