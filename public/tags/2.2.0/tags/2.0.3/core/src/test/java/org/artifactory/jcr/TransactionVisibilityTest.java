package org.artifactory.jcr;

import org.apache.jackrabbit.core.XASessionImpl;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.transaction.xa.Xid;

@Test
public class TransactionVisibilityTest extends RepositoryTestBase {

    public void testTxCreateVisbilityFail() throws Exception {
        XASessionImpl session = (XASessionImpl) getRepository().login();
        Xid xid1 = new DummyXid((byte) 1);
        beginTx(xid1, session);
        Node root = session.getRootNode();
        root.addNode("a", "nt:unstructured");
        session.save();
        try {
            root.getNode("a");
        } catch (RepositoryException e) {
            fail("Created node not found!");
        }
        //Search the new node
        Node newNode = findNewNode(session);
        // accordind to jcr spec we cannot search non committed objects
        assertNull(newNode);
        //assertEquals("a", newNode.getName());
        //Commit and search again
        commitTx(xid1, session);
        //Try to search it again - this time it will work!
        Xid xid2 = new DummyXid((byte) 1);
        beginTx(xid2, session);
        newNode = findNewNode(session);
        assertNotNull(newNode);
        assertEquals("a", newNode.getName());
        commitTx(xid2, session);
        //Finally logout
        session.logout();
    }

    public void testNonTxCreateVisbilityOk() throws Exception {
        Session session = getRepository().login();
        Node root = session.getRootNode();
        root.addNode("a", "nt:unstructured");
        session.save();
        try {
            root.getNode("a");
        } catch (RepositoryException e) {
            fail("Created node not found!");
        }
        //Search the new node
        Node newNode = findNewNode(session);
        assertNotNull(newNode);
        assertEquals("a", newNode.getName());
        session.logout();
    }

    public void testTXCreateVisbilityOk() throws Exception {
        XASessionImpl session = (XASessionImpl) getRepository().login();
        Xid xid2 = new DummyXid((byte) 1);
        beginTx(xid2, session);
        Node root = session.getRootNode();
        root.addNode("a", "nt:unstructured");
        session.save();
        commitTx(xid2, session);
        //Try to search again
        Xid xid3 = new DummyXid((byte) 1);
        beginTx(xid3, session);
        Node newNode = findNewNode(session);
        assertNotNull(newNode);
        assertEquals("a", newNode.getName());
        commitTx(xid3, session);
        //Finally logout
        session.logout();
    }

    private static Node findNewNode(Session session) throws RepositoryException {
        Workspace workSpace = session.getWorkspace();
        QueryManager queryManager = workSpace.getQueryManager();
        String queryStr = "/jcr:root//element(*, nt:unstructured)";
        Query query = queryManager.createQuery(queryStr, Query.XPATH);
        QueryResult queryResult = query.execute();
        NodeIterator nodes = queryResult.getNodes();
        if (nodes.hasNext()) {
            return nodes.nextNode();
        } else {
            return null;
        }
    }
}
