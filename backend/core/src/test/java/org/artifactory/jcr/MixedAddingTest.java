/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

import org.apache.jackrabbit.core.XASessionImpl;
import org.testng.annotations.Test;

import javax.jcr.Node;
import javax.transaction.xa.Xid;

public class MixedAddingTest extends RepositoryTestBase {

    @Test
    public void testTxMixedAdd() throws Exception {
        XASessionImpl session1 = login();
        //Create the node
        Xid xid1 = new DummyXid((byte) 1);
        beginTx(xid1, session1);
        Node root = session1.getRootNode();
        Node a1 = root.addNode("a", "nt:unstructured");
        a1.addNode("b", "nt:unstructured");
        session1.save();
        XASessionImpl session2 = login();
        //Create the node
        Xid xid2 = new DummyXid((byte) 2);
        beginTx(xid2, session2);
        Node a2 = root.getNode("a");
        a2.addNode("c", "nt:unstructured");
        commitTx(xid1, session1);
        commitTx(xid2, session2);
        session2.logout();
        session1.logout();

        /*
        //Lock it
        xid = new DummyXid((byte) 2);
        beginTx(xid, session1);
        root = session1.getRootNode();
        try {
             a = root.getNode("a");
        } catch (RepositoryException e) {
            fail("Created node not found!");
        }
        String token = NodeLock.lock(a);
        commitTx(xid, session1);
        session1.logout();

        Xid xid2 = new DummyXid((byte) 1);
        beginTx(xid2, session1);
        newNode = findNewNode(session1);
        assertNotNull(newNode);
        assertEquals("a", newNode.getName());
        commitTx(xid2, session1);
        //Finally logout
        session1.logout();*/
    }
}