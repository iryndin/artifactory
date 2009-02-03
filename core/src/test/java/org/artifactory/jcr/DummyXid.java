package org.artifactory.jcr;

import org.apache.log4j.Logger;

import javax.transaction.xa.Xid;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class DummyXid implements Xid {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(DummyXid.class);

    private byte gtxid;

    public DummyXid(byte gtxid) {
        this.gtxid = gtxid;
    }

    public byte[] getGlobalTransactionId() {
        return new byte[]{gtxid};
    }

    public int getFormatId() {
        return 0;
    }

    public byte[] getBranchQualifier() {
        return new byte[0];
    }
}
