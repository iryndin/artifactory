package org.artifactory.search;

import org.apache.log4j.Logger;
import org.artifactory.jcr.ArtifactoryJcrConstants;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrFile;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.spring.ArtifactoryContext;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SearchHelper {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SearchHelper.class);

    @SuppressWarnings({"unchecked"})
    public static Set<ArtifactResource> searchArtifacts(
            SearchControls controls, ArtifactoryContext context) {
        Set<ArtifactResource> results = new HashSet<ArtifactResource>();
        String search = controls.getSearch();
        final String wildcard = "*" + controls.getSearch() + "*";
        if (search != null) {
            JcrHelper jcrHelper = context.getCentralConfig().getJcr();
            SearchCallback callback = new SearchCallback(wildcard);
            results = jcrHelper.doInSession(callback);
        }
        return results;
    }

    private static class SearchCallback implements JcrCallback<Set<ArtifactResource>> {
        private String wildcard;

        public SearchCallback(String wildcard) {
            this.wildcard = wildcard;
        }

        public Set<ArtifactResource> doInJcr(JcrSessionWrapper session) throws RepositoryException {
            Set<ArtifactResource> results = new HashSet<ArtifactResource>();
            Workspace workSpace = session.getWorkspace();
            QueryManager queryManager = workSpace.getQueryManager();
            String queryStr =
                    "/jcr:root//element(*, " + ArtifactoryJcrConstants.NT_ARTIFACTORY_FILE +
                            ")[jcr:contains(@" + ArtifactoryJcrConstants.PROP_ARTIFACTORY_NAME +
                            ", '" + wildcard + "')]";
            /*String queryStr = "/jcr:root//project//jcr:xmltext"
                    + "[jcr:contains(., '" + wildcard + "')]";*/
            Query query = queryManager.createQuery(queryStr, Query.XPATH);
            QueryResult queryResult = query.execute();
            NodeIterator nodes = queryResult.getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                String absPath = node.getPath();
                int relPathEnd = absPath.indexOf("/artifactory:xml/");
                if (relPathEnd == -1) {
                    relPathEnd = absPath.length();
                }
                String relPath = absPath.substring(1, relPathEnd);
                Node artifactNode = session.getRootNode().getNode(relPath);
                JcrFile jcrFile = new JcrFile(artifactNode);
                ArtifactResource artifact = new ArtifactResource(jcrFile);
                if (artifact.isStandardPackaging()) {
                    results.add(artifact);
                }
            }
            return results;
        }
    }
}
