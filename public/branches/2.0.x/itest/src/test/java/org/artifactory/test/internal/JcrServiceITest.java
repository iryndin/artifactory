package org.artifactory.test.internal;

import org.artifactory.api.repo.ArtifactCount;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.utils.JcrNodeTraversal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

/**
 * Integration tests mainly for the JcrService.
 *
 * @author Yossi Shaul
 */
@Test
public class JcrServiceITest extends ArtifactoryTestBase {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger log = LoggerFactory.getLogger(JcrServiceITest.class);

    protected JcrService jcrService;

    @BeforeClass
    public void setup() throws IOException, InterruptedException, URISyntaxException {
        importToRepoFromExportPath("/export/WcTest", "libs-releases-local", false);
        jcrService = context.beanForType(JcrService.class);
    }

    public void countArtifacts() throws Exception {
        ArtifactCount count = jcrService.getArtifactCount();

        assertNotNull(count);
        assertEquals(count.getNumberOfJars(), 0, "No jars deployed");
        assertEquals(count.getNumberOfPoms(), 1, "Expecting only one deployed pom");
    }

    public void reposXpathQuery() throws Exception {
        String allRepos = "/jcr:root/repositories/*";
        QueryResult result = jcrService.executeXpathQuery(allRepos);

        assertNotNull(result);
        NodeIterator iterator = result.getNodes();
        assertEquals(iterator.getSize(), 4, "Expecting 4 repos");

        Node repoNode = iterator.nextNode();
        assertEquals(getStringProperty(repoNode, "jcr:primaryType"), JcrFolder.NT_ARTIFACTORY_FOLDER);
        assertEquals(repoNode.getPrimaryNodeType().getName(), JcrFolder.NT_ARTIFACTORY_FOLDER);
        assertEquals(getStringProperty(repoNode, "artifactory:name"), "");
    }

    public void versionXpathQuery() throws Exception {
        String allVersionNodesWithXmlCharacters =
                "/jcr:root/repositories//*/_pre:project/_pre:version/jcr:xmltext[@jcr:xmlcharacters]";
        QueryResult result = jcrService.executeXpathQuery(allVersionNodesWithXmlCharacters);

        assertNotNull(result);
        NodeIterator iterator = result.getNodes();
        assertEquals(iterator.getSize(), 1, "Only one pom deployed and has a version");
        Node versionNode = iterator.nextNode();
        Property versionProperty = versionNode.getProperty("jcr:xmlcharacters");
        assertEquals(versionProperty.getValue().getString(), "0.0.227", "Version mismatch");
    }

    @Test(enabled = true)
    public void printJcrTree() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        JcrSession session = jcrService.getManagedSession();
        Node root = session.getRootNode();

        JcrNodeTraversal.preorder(root, Arrays.asList("jcr:system"));
        TransactionSynchronizationManager.clear();
    }

    private String getStringProperty(Node repoNode, String propertyName)
            throws RepositoryException {
        String getStringProperty = repoNode.getProperty(propertyName).getString();
        return getStringProperty;
    }

    @Override
    String getConfigName() {
        return "no-remote-repo";
    }

}
