package org.artifactory.jcr;

import org.apache.jackrabbit.core.XASessionImpl;
import org.testng.annotations.Test;

import javax.jcr.Node;
import javax.transaction.xa.Xid;

public class MixedAddingTest extends RepositoryTestBase {

    @Test
    public void testTxMIxedAdd() throws Exception {
        XASessionImpl session1 = (XASessionImpl) getRepository().login();
        //Create the node
        Xid xid1 = new DummyXid((byte) 1);
        beginTx(xid1, session1);
        Node root = session1.getRootNode();
        Node a1 = root.addNode("a", "nt:unstructured");
        a1.addNode("b", "nt:unstructured");
        session1.save();
        XASessionImpl session2 = (XASessionImpl) getRepository().login();
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