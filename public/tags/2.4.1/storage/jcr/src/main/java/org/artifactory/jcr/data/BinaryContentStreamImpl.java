package org.artifactory.jcr.data;

import org.apache.commons.io.IOUtils;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.io.checksum.ChecksumInputStream;
import org.artifactory.io.checksum.Checksums;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Random;


/**
 * Date: 8/5/11
 * Time: 12:26 AM
 *
 * @author Fred Simon
 */
public class BinaryContentStreamImpl extends BinaryContentBase {
    private static final Logger log = LoggerFactory.getLogger(BinaryContentStreamImpl.class);

    private static final Random random = new Random(System.nanoTime());

    private final String tempId = "" + random.nextLong();
    private ChecksumInputStream inputStream;

    public BinaryContentStreamImpl(String mimeType, String encoding, InputStream inputStream) {
        super(mimeType, encoding);
        this.inputStream = new ChecksumInputStream(inputStream, JcrVfsHelper.getChecksumsToCompute());
    }

    public String binaryId() {
        if (!inputStream.isClosed()) {
            return tempId;
        }
        return super.binaryId();
    }

    @Override
    public ChecksumsInfo getChecksums() {
        if (inputStream.isClosed() && checksumsInfo.getChecksums().isEmpty()) {
            Checksums.fillChecksumInfo(checksumsInfo, inputStream.getChecksums());
        }
        return checksumsInfo;
    }

    public long getSize() {
        return inputStream.getTotalBytesRead();
    }

    public InputStream getStream() {
        return inputStream;
    }

    public void checkClosed() {
        IOUtils.closeQuietly(inputStream);
        Checksums.fillChecksumInfo(checksumsInfo, inputStream.getChecksums());
    }
}
