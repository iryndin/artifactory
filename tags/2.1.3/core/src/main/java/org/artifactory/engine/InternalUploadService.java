package org.artifactory.engine;

import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.Request;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.request.UploadService;
import org.artifactory.repo.jcr.StoringRepo;

import java.io.IOException;

/**
 * The internal implementation of the upload service
 *
 * @author Noam Y. Tenne
 */
public interface InternalUploadService extends UploadService {

    /**
     * Performs the actual uploading proccess
     *
     * @param request  Originating request
     * @param response Response to send
     * @param repo     Target repo
     * @param path     Path of upload
     * @throws IOException
     */
    @Lock(transactional = true)
    @Request(aggregateEventsByTimeWindow = true)
    void doProcess(ArtifactoryRequest request, ArtifactoryResponse response, StoringRepo repo, String path)
            throws IOException;
}
