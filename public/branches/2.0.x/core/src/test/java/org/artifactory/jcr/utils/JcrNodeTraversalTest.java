package org.artifactory.jcr.utils;

import org.artifactory.jcr.RepositoryTestBase;
import org.testng.annotations.Test;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Arrays;

/**
 * @author Yossi Shaul
 */
@Test(enabled = false)
public class JcrNodeTraversalTest extends RepositoryTestBase {

    public void print() throws RepositoryException {
        Session session = getRepository().login();
        JcrNodeTraversal.preorder(session.getRootNode());
        session.logout();
    }

    public void printExcludeSystem() throws RepositoryException {
        Session session = getRepository().login();
        JcrNodeTraversal.preorder(session.getRootNode(), Arrays.asList("jcr:system"));
        session.logout();
    }

}
