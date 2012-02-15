package org.artifactory.jcr.data;

import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.log.LoggerFactory;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.data.BinaryContent;
import org.slf4j.Logger;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import java.io.InputStream;

/**
 * Date: 8/5/11
 * Time: 1:37 AM
 *
 * @author Fred Simon
 */
public class BinaryContentJcrImpl extends BinaryContentBase {
    private static final Logger log = LoggerFactory.getLogger(BinaryContentJcrImpl.class);

    private final Binary binary;

    public BinaryContentJcrImpl(String mimeType, String encoding,
            ChecksumsInfo checksums, Binary jcrBinary) {
        super(mimeType, encoding, checksums);
        this.binary = jcrBinary;
    }

    public BinaryContentJcrImpl(BinaryContent origVfsBinary, Binary jcrBinary) {
        super(origVfsBinary);
        this.binary = jcrBinary;
    }

    @Override
    public long getSize() {
        try {
            return binary.getSize();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public InputStream getStream() {
        try {
            return binary.getStream();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public Binary getBinary() {
        return binary;
    }

    @Override
    public void checkClosed() {
        // Nothing here it's the other side needs check
    }
}
