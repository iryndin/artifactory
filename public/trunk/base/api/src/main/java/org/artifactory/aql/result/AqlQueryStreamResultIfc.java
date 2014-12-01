package org.artifactory.aql.result;

import java.io.IOException;

/**
 * @author Gidi Shabat
 */
public interface AqlQueryStreamResultIfc {
    int read() throws IOException;

    void close() throws IOException;
}
