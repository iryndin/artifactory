/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

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
