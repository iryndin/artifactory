package org.artifactory.test.internal;

import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.utils.JcrNodeTraversal;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

/**
 * Simple console to test xpath and sql queries against Artifactory's JCR. Don't include in the test
 * suit, this test is interactive.
 *
 * @author Yossi Shaul
 */
public class JcrConsole extends ArtifactoryTestBase {
    @BeforeClass
    public void setupWcDir() throws IOException, InterruptedException, URISyntaxException {
        importToRepoFromExportPath("/export/WcTest", "libs-releases-local", false);
    }

    @Test(enabled = true)
    public void queryConsole() throws Exception {
        JcrService jcrService = context.beanForType(JcrService.class);
        Repository repository = jcrService.getRepository();
        Session session = repository.login();
        Workspace workspace = session.getWorkspace();
        QueryManager qm = workspace.getQueryManager();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String queryType = Query.XPATH;
        System.out.printf("Query console started.%n" +
                "Type your query or one of the special commands:%n" +
                "quit    Quite the query console%n" +
                "sql     Switch to sql query mode%n" +
                "xpath   Switch to xpath query mode%n" +
                "dump    Prints the whole jcr tree%n"
        );
        while (true) {
            System.out.print(queryType + "-query> ");
            String queryString = reader.readLine();
            if (queryString.equals("quit")) {
                break;
            }
            if (queryString.length() == 0 || queryString.startsWith("#")) {
                continue;
            }
            if (queryString.equals(Query.XPATH) || queryString.equals(Query.SQL)) {
                queryType = queryString;
                continue;
            }
            if (queryString.equals("dump")) {
                JcrNodeTraversal.preorder(session.getRootNode());
                continue;
            }
            try {
                int resultCounter = 0;
                Query query = qm.createQuery(queryString, queryType);
                QueryResult queryResult = query.execute();
                NodeIterator nodeIterator = queryResult.getNodes();
                while (nodeIterator.hasNext()) {
                    Node node = nodeIterator.nextNode();
                    System.out.println(JcrNodeTraversal.nodeToString(node));
                    resultCounter++;
                }
                System.out.println("#results: " + resultCounter);
            } catch (Exception e) {
                System.err.printf("error: %s%n", e.getMessage());
            }

        }// main loop
        session.logout();
    }


    ///* Sample queries:
    //    # all nodes of primary node type 'artifactory:folder' including sub types
    //    //element(*, artifactory:folder)
    //    # all 'artifactory:folder' who has artifactory:name property and jcr:created
    //    //element(*, artifactory:folder)[@artifactory:name and @jcr:created]
    //    # all artifactory repositories
    //    //repositories/*
    //    # all artifactory folders
    //    /jcr:root/repositories//element(*, artifactory:folder)
    //    # all artifactory folders under the repositories
    //    /jcr:root/repositories/*//element(*, artifactory:folder)
    //*/  //element(*, artifactory:file)[jcr:like[@artifactory:name, '%.pom']
    //   //element(*, artifactory:file) [jcr:contains(., '.pom')] order by jcr:score() descending
    //   //element(*, artifactory:file) [jcr:like(@artifactory:name,'%.pom')] order by jcr:score() descending
    //   /jcr:root/repositories/libs-releases-local/biz//*/element(*, artifactory:file) [jcr:like(@artifactory:name,'%.pom')] order by jcr:score() descending
    //   /jcr:root/repositories/libs-releases-local/biz//*/element(*, artifactory:file) [jcr:like(@artifactory:name,'%.pom')]/artifactory:metadata


    // /jcr:root/repositories/*/biz/aQute
    // /jcr:root/repositories//*/artifactory.stats
    // /jcr:root/repositories//*/_pre:version/jcr:xmltext[@jcr:xmlcharacters]
    // /jcr:root/repositories//*/_pre:project/_pre:version/jcr:xmltext[@jcr:xmlcharacters]
    // /jcr:root/repositories//*/_pre:project/_pre:parent/_pre:version/jcr:xmltext[@jcr:xmlcharacters]
    //*/version[@jcr:xmlcharacters]

    @Override
    String getConfigName() {
        return "no-remote-repo";
    }
}
