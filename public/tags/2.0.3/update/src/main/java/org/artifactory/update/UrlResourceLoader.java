package org.artifactory.update;

import org.artifactory.common.ResourceStreamHandle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * User: freds
 * Date: Jun 1, 2008
 */
public class UrlResourceLoader implements ResourceStreamHandle {
    private final URL url;
    private InputStream is;

    public UrlResourceLoader(URL url) {
        this.url = url;
    }

    public InputStream getInputStream() {
        if (is == null) {
            try {
                is = url.openStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return is;
    }

    public void close() {
        try {
            if (is != null) is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            is = null;
        }
    }
}
