/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.NullWriter;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.common.StatusHolder;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * An internal response that is used as a dummy client to consume responses from Artifactory. It is used when
 * Artifactory is sending itself a request (e.g., to eager fetch sources jar)
 *
 * @author Yossi Shaul
 */
public class InternalArtifactoryResponse extends ArtifactoryResponseBase {

    private String statusMessage;

    public InternalArtifactoryResponse() {
    }

    public void setLastModified(long lastModified) {
        // ignore
    }

    public void setEtag(String etag) {
        // ignore
    }

    public void setMd5(String md5) {
        // ignore
    }

    public void setSha1(String sha1) {
        // ignore
    }

    @Override
    public void sendErrorInternal(int statusCode, String reason) throws IOException {
        // nothing special
        statusMessage = reason;
    }

    public void sendAuthorizationRequired(String message, String realm) throws IOException {
        // ignore
    }

    public OutputStream getOutputStream() throws IOException {
        return new NullOutputStream();
    }

    public Writer getWriter() throws IOException {
        return new NullWriter();
    }

    public void setHeader(String header, String value) {
        // ignore
    }

    public void flush() {
        // ignore
    }

    public void setContentType(String contentType) {
        // ignore
    }

    public boolean isCommitted() {
        return false;
    }

    @Override
    public void sendError(int statusCode, String reason, Logger log) throws IOException {
        log.info("Internal request failed with code {}. Reason: {}", statusCode, reason);
        super.sendError(statusCode, reason, log);
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public StatusHolder getStatusHolder() {
        BasicStatusHolder sh = new BasicStatusHolder();
        if (isSuccessful()) {
            sh.setStatus(statusMessage, getStatus(), null);
        } else {
            sh.setError(statusMessage, getStatus(), getException(), null);
        }
        return sh;
    }

    @Override
    public void clearState() {
        super.clearState();
        statusMessage = null;
    }
}
