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

package org.artifactory.repo.remote.browse;

import com.google.common.base.Charsets;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.artifactory.api.storage.StorageUnit;
import org.artifactory.request.RemoteRequestException;
import org.artifactory.util.HttpUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Abstract class for remote repository browsing.
 *
 * @author Yossi Shaul
 */
public abstract class RemoteRepositoryBrowser {

    protected final HttpExecutor client;

    public RemoteRepositoryBrowser(HttpExecutor client) {
        this.client = client;
    }

    public abstract List<RemoteItem> listContent(String url) throws IOException;

    protected String getContent(String url) throws IOException {
        // add trailing slash for relative urls
        if (!url.endsWith("/")) {
            url += "/";
        }

        GetMethod method = new GetMethod(url);
        int statusCode = client.executeMethod(method);
        try {
            assertSizeLimit(url, method);

            InputStream responseBodyAsStream = HttpUtils.getGzipAwareResponseStream(method);
            String responseString = IOUtils.toString(responseBodyAsStream, Charsets.UTF_8.name());
            if (statusCode != HttpStatus.SC_OK) {
                String message = "Unable to retrieve " + url + ": "
                        + method.getStatusCode() + ": " + method.getStatusText();
                throw new RemoteRequestException(message, statusCode, responseString);
            }

            return responseString;
        } finally {
            method.releaseConnection();
        }
    }

    protected void assertSizeLimit(String urlStr, GetMethod method) throws IOException {
        if (HttpUtils.getContentLength(method) > StorageUnit.MB.toBytes(1)) {
            throw new IOException("Failed to retrieve directory listing from " + urlStr
                    + ". Response Content-Length of " + HttpUtils.getContentLength(method)
                    + " exceeds max of " + StorageUnit.MB.toBytes(1) + " bytes.");
        }
    }

    protected String forceDirectoryUrl(String url) {
        // add trailing slash we are dealing with directories
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url;
    }
}
