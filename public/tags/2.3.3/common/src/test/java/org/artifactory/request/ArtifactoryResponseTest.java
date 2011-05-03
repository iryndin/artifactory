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

import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import static org.testng.Assert.assertTrue;

/**
 * @author Yoav Landman
 */
@Test
public class ArtifactoryResponseTest {

    private static final Logger log = LoggerFactory.getLogger(ArtifactoryResponseTest.class);

    public void sendErroChangesState() throws IOException {
        ArtifactoryResponseBase response = newResponse();
        response.sendError(500, "Bad bad thing", log);
        assertTrue(response.isError());
    }

    private ArtifactoryResponseBase newResponse() {
        return new ArtifactoryResponseBase() {
            @Override
            protected void sendErrorInternal(int code, String reason) throws IOException {
            }

            public void setLastModified(long lastModified) {
            }

            public void setEtag(String etag) {
            }

            public void setSha1(String sha1) {
            }

            public void setMd5(String md5) {
            }

            public void setContentType(String contentType) {
            }

            public OutputStream getOutputStream() throws IOException {
                return null;
            }

            public Writer getWriter() throws IOException {
                return null;
            }

            public void setHeader(String header, String value) {
            }

            public boolean isCommitted() {
                return false;
            }

            public void flush() {
            }

            public void sendAuthorizationRequired(String message, String realm) throws IOException {
            }
        };
    }
}
