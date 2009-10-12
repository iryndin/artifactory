package org.artifactory.jcr;

import javax.transaction.xa.Xid;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class DummyXid implements Xid {
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
