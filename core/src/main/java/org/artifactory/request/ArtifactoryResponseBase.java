package org.artifactory.request;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class ArtifactoryResponseBase implements ArtifactoryResponse {

    private Success success = Success.unset;

    public void sendStream(InputStream is) throws IOException {
        OutputStream os = getOutputStream();
        if (success.equals(Success.unset)) {
            success = Success.success;
        }
        try {
            IOUtils.copy(is, os);
            sendOk();
        } catch (Exception e) {
            sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }
    }

    public void sendFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        sendStream(is);
    }

    public void sendError(int statusCode) throws IOException {
        success = Success.failure;
        sendErrorInternal(statusCode);
    }

    public boolean isSuccessful() {
        return success.equals(Success.success);
    }

    protected abstract void sendErrorInternal(int code) throws IOException;
}