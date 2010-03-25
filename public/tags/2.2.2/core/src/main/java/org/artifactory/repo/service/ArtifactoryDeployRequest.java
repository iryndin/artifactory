/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.repo.service;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.request.InternalArtifactoryRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @author Tomer Cohen
 */
public class ArtifactoryDeployRequest extends InternalArtifactoryRequest {

    private InputStream inputStream;
    private int contentLength;
    private long lastModified;

    public ArtifactoryDeployRequest(RepoPath pathToUpload, File fileToUpload) throws FileNotFoundException {
        super(pathToUpload);
        this.inputStream = new FileInputStream(fileToUpload);
        contentLength = (int) fileToUpload.length();
        lastModified = fileToUpload.lastModified();
    }

    public ArtifactoryDeployRequest(RepoPath pathToUpload, InputStream inputStream, long contentLength,
            long lastModified) {
        super(pathToUpload);
        this.inputStream = inputStream;
        this.contentLength = (int) contentLength;
        this.lastModified = lastModified;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public int getContentLength() {
        return contentLength;
    }
}
