/*
 * This file is part of Artifactory.
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

    public InternalArtifactoryResponse() {
    }

    public void setLastModified(long lastModified) {
        // ignore
    }

    public void setEtag(String etag) {
        // ignore
    }

    @Override
    public void sendErrorInternal(int statusCode, String reason) throws IOException {
        // nothing special
    }

    public void sendAuthorizationRequired(String message, String realm) throws IOException {
        // ignore
    }

    public int getContentLength() {
        return -1;
    }

    public boolean isContentLengthSet() {
        return false;
    }

    public void setContentLength(int length) {
        // ignore
    }

    public OutputStream getOutputStream() throws IOException {
        return new NullOutputStream();
    }

    public Writer getWriter() throws IOException {
        return new NullWriter();
    }

    public void setStatus(int status) {
        // ignore
    }

    public void setHeader(String header, String value) {
        // ignore
    }

    public void sendOk() {
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
    public void sendError(int statusCode, String reason, Logger logger) throws IOException {
        logger.info("Eager download failed with code {}. Reason: {}", statusCode, reason);
    }

}
