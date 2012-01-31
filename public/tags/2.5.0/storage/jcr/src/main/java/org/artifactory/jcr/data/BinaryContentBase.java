package org.artifactory.jcr.data;

import org.apache.commons.io.IOUtils;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.data.BinaryContent;
import org.artifactory.sapi.data.MutableBinaryContent;

import java.io.IOException;

/**
 * Date: 8/5/11
 * Time: 1:43 AM
 *
 * @author Fred Simon
 */
public abstract class BinaryContentBase implements MutableBinaryContent {
    private final String mimeType;
    private final String encoding;
    protected final ChecksumsInfo checksumsInfo;
    protected String stringContent;
    protected long lastModified = System.currentTimeMillis();

    protected BinaryContentBase(String mimeType, String encoding) {
        this.mimeType = mimeType;
        this.encoding = encoding;
        checksumsInfo = new ChecksumsInfo();
    }

    public BinaryContentBase(BinaryContent original) {
        BinaryContentBase cb = (BinaryContentBase) original;
        this.mimeType = cb.getMimeType();
        this.encoding = cb.getEncoding();
        this.checksumsInfo = cb.getChecksums();
        this.stringContent = cb.stringContent;
    }

    public BinaryContentBase(String mimeType, String encoding, ChecksumsInfo checksums) {
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.checksumsInfo = checksums;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public final String getMimeType() {
        return mimeType;
    }

    @Override
    public final String getEncoding() {
        return encoding;
    }

    @Override
    public ChecksumsInfo getChecksums() {
        return checksumsInfo;
    }

    @Override
    public String getContentAsString() {
        if (stringContent == null) {
            try {
                stringContent = IOUtils.toString(getStream(), encoding);
            } catch (IOException e) {
                throw new RepositoryRuntimeException(e);
            }
        }
        return stringContent;
    }

    @Override
    public String binaryId() {
        return getChecksums().getSha1();
    }
}
