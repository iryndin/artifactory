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
        String mimeType = RequestResponseHelper.getMimeType(res.getRelPath());
        response.setContentType(mimeType);
        response.setContentLength((int) res.getSize());
        response.setLastModified(res.getLastModifiedTime());
    }
}
