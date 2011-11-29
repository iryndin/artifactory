package org.artifactory.jcr.data;

import org.apache.commons.io.IOUtils;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumInputStream;
import org.artifactory.io.checksum.Checksums;
import org.artifactory.log.LoggerFactory;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;


/**
 * Date: 8/5/11
 * Time: 12:26 AM
 *
 * @author Fred Simon
 */
public class BinaryContentStringImpl extends BinaryContentBase {
    private static final Logger log = LoggerFactory.getLogger(BinaryContentStringImpl.class);

    private final long size;
    protected byte[] bytes;

    public BinaryContentStringImpl(String mimeType, String encoding, String stringContent) {
        super(mimeType, encoding);
        this.stringContent = stringContent;
        try {
            bytes = stringContent.getBytes(getEncoding());
            size = bytes.length;
            Checksum[] checksums = JcrVfsHelper.getChecksumsToCompute();
            ChecksumInputStream is = new ChecksumInputStream(getStream(), checksums);
            String result = IOUtils.toString(is);
            // TODO: Check the result equals the origin
            IOUtils.closeQuietly(is);
            Checksums.fillChecksumInfo(getChecksums(), checksums);
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryRuntimeException(e);
        } catch (IOException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public long getSize() {
        return size;
    }

    public InputStream getStream() {
        return new ByteArrayInputStream(bytes);
    }

    public void checkClosed() {
        // Nothing to close
    }
}
