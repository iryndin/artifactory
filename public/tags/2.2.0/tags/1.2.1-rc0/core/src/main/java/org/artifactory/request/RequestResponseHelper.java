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
import org.apache.log4j.Logger;
import org.artifactory.engine.ResourceStreamHandle;
import org.artifactory.resource.RepoResource;
import org.artifactory.utils.MimeTypes;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public final class RequestResponseHelper {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(RequestResponseHelper.class);

    public static void sendBodyResponse(
            ArtifactoryResponse response, RepoResource res, ResourceStreamHandle handle)
            throws IOException {
        try {
            updateResponseFromRepoResource(response, res);
            response.sendStream(handle.getInputStream());
        } finally {
            handle.close();
        }
    }

    public static void sendHeadResponse(ArtifactoryResponse response, RepoResource res) {
        LOGGER.info(res.getRepoKey() + ": Sending HEAD meta-information");
        updateResponseFromRepoResource(response, res);
        response.sendOk();
    }

    public static String getMimeType(String path) {
        MimeTypes.MimeType mimeType = MimeTypes.getMimeTypeByPath(path);
        if (mimeType == null) {
            return "application/octet-stream";
        }
        return mimeType.getMimeType();
    }

    public static void sendNotModifiedResponse(
            ArtifactoryResponse response, RepoResource res) throws IOException {
        LOGGER.info(res.getRepoKey() + ": Sending NOT-MODIFIED response");
        updateResponseFromRepoResource(response, res);
        response.sendError(HttpStatus.SC_NOT_MODIFIED);
    }

    private static void updateResponseFromRepoResource(ArtifactoryResponse response,
            RepoResource res) {
        String mimeType = RequestResponseHelper.getMimeType(res.getPath());
        response.setContentType(mimeType);
        response.setContentLength((int) res.getSize());
        response.setLastModified(res.getLastModifiedTime());
    }
}
