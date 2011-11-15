/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.transaction.xa.Xid;

import static org.testng.Assert.*;

@Test(sequential = false)
public class TransactionVisibilityTest extends RepositoryTestBase {

    //@Test(invocationCount = 3, threadPoolSize = 3, enabled = false)
    public void testVisibilityAfterMove() throws Exception {
        XASessionImpl session = login();
        Xid xid1 = new DummyXid((byte) 1);
        beginTx(xid1, session);
        Node root = session.getRootNode();
        String aName = "a" + Thread.currentThread().getId();
        String bName = "b" + Thread.currentThread().getId();
        root.addNode(aName, "nt:unstructured");
        root.addNode(bName, "nt:unstructured");
        session.save();
        commitTx(xid1, session);

        Xid xid2 = new DummyXid((byte) 2);
        beginTx(xid2, session);
        root = session.getRootNode();
        Node a = root.getNode(aName);
        session.move(a.getPath(), "/" + bName + "/" + aName);
        try {
            root.getNode(aName);
            root.getNode(aName); //Try again for debugging
            fail("a should not be found");
        } catch (RepositoryException e) {
            //Should not be found
        }
        commitTx(xid2, session);
        session.logout();
    }

    public void testTxCreateVisbilityFail() throws Exception {
        XASessionImpl session = login();
        Xid xid1 = new DummyXid((byte) 1);
        beginTx(xid1, session);
        Node root = session.getRootNode();
        root.addNode("a", "nt:unstructured");
        try {
            root.getNode("a");
        } catch (RepositoryException e) {
            fail("Created node not found!");
        }
        //Search the new node
        Node newNode = findNewNode(session);
        // according to jcr spec we cannot search non committed objects
        assertNull(newNode);
        //assertEquals("a", newNode.getName());
        //Commit and search again
        session.save();
        commitTx(xid1, session);
        //Try to search it again - this time it will work!
        Xid xid2 = new DummyXid((byte) 1);
        beginTx(xid2, session);
        newNode = findNewNode(session);
        assertNotNull(newNode);
        assertEquals("a", newNode.getName());
        commitTx(xid2, session);
        //Finally log out
        session.logout();
    }

    public void testNonTxCreateVisbilityOk() throws Exception {
        Session session = login();
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

    public void testTxCreateVisbilityOk() throws Exception {
        XASessionImpl session = login();
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
        //Finally log out
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
