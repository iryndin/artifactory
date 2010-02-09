package org.artifactory.engine;

import java.io.InputStream;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface ResourceStreamHandle {
    InputStream getInputStream();
    void close();
}
