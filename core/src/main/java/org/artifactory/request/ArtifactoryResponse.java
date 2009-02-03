package org.artifactory.request;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ArtifactoryResponse {
    void setLastModified(long lastModified);

    void setContentLength(int length);

    void setContentType(String contentType);

    public OutputStream getOutputStream() throws IOException;

    void sendError(int statusCode) throws IOException;

    void sendStream(InputStream is) throws IOException;

    void sendFile(File targetFile) throws IOException;

    void sendOk();

    boolean isCommited();
}