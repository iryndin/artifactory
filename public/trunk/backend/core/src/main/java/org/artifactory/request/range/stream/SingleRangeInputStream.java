package org.artifactory.request.range.stream;

import org.apache.commons.io.IOUtils;
import org.artifactory.request.range.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Gidi Shabat
 */
public class SingleRangeInputStream extends InputStream {
    public static final Logger log = LoggerFactory.getLogger(SingleRangeSkipInputStream.class);
    private long left;
    private InputStream inputStream;

    public SingleRangeInputStream(Range range, InputStream inputStream) throws IOException {
        // Skip unwanted bites
        for (int i = 0; i < range.getStart(); i++) {
            // Skipping single byte;
            // noinspection ResultOfMethodCallIgnored
            inputStream.read();
        }
        // Limit the stream
        left = range.getEnd() - range.getStart() + 1;
        this.inputStream =inputStream;
    }

    @Override
    public int read() throws IOException {

        if (left == 0) {
            // Make sure to read the file until EOF
            int result=inputStream.read();
            while (result>=0){
                result=inputStream.read();
            }
            return result;
        }
        int result = inputStream.read();
        if (result != -1) {
            --left;
        }
        return result;
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(inputStream);
    }

}
