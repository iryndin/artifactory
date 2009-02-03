package org.artifactory.request;

import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.utils.IoUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class ArtifactoryResponseBase implements ArtifactoryResponse {


    public void sendStream(InputStream is) throws IOException {
        OutputStream os = getOutputStream();
        try {
            IoUtil.transferStream(is, os);
            sendOk();
        } catch (Exception e) {
            sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } finally {
            IoUtil.close(os);
            IoUtil.close(is);
        }
    }

    public void sendFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        sendStream(is);
    }
}