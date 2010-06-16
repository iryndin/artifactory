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

package org.artifactory.engine;

import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.Request;
import org.artifactory.api.repo.exception.RepoRejectionException;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.request.UploadService;
import org.artifactory.repo.LocalRepo;

import java.io.IOException;

/**
 * The internal implementation of the upload service
 *
 * @author Noam Y. Tenne
 */
public interface InternalUploadService extends UploadService {

    /**
     * Performs the actual uploading process.
     *
     * @param request  Originating request
     * @param response Response to send
     * @param repo     Target local non-cahce repo
     */
    @Lock(transactional = true)
    @Request(aggregateEventsByTimeWindow = true)
    void doProcess(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo) throws IOException,
            RepoRejectionException;
}
