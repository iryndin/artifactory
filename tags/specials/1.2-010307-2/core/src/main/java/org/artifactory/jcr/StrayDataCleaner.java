package org.artifactory.jcr;

import org.apache.log4j.Logger;
import org.artifactory.maven.MavenUtils;
import org.artifactory.request.ArtifactoryRequest;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class StrayDataCleaner implements JcrCallback<Object> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(StrayDataCleaner.class);

    private final String queryString;

    public StrayDataCleaner(ArtifactoryRequest request) {
        String dir = request.getDir();
        /*
        //Encode numbers in the dir path, according to sec 6.4.3 of the jcr spec
        //See also the discussion at http://issues.apache.org/jira/browse/JCR-579
        String encodedDir = dir != null ? ISO9075.encode(dir) : "*";
        this.queryString = "/jcr:root*/
        /*//*" + encodedDir +
        "//element(*, " + ArtifactoryJcrConstants.NT_ARTIFACTORY_FILE + ")" +
        "[@" + ArtifactoryJcrConstants.PROP_ARTIFACTORY_NAME + "='maven-metadata.xml']";
        */
        this.queryString = "SELECT * FROM " + ArtifactoryJcrConstants.NT_ARTIFACTORY_FILE +
                " WHERE jcr:path LIKE '/%/" + dir + "/%'" +
                " AND " + ArtifactoryJcrConstants.PROP_ARTIFACTORY_NAME + "='maven-metadata.xml'";
    }

    public Object doInJcr(JcrSessionWrapper session) throws RepositoryException {
        Set<Node> results = new HashSet<Node>();
        Workspace workSpace = session.getWorkspace();
        QueryManager queryManager = workSpace.getQueryManager();
        //Find all the metadata.ext files and check that their parent has real data
        Query query = queryManager.createQuery(queryString, Query.SQL);
        QueryResult queryResult = query.execute();
        NodeIterator nodes = queryResult.getNodes();
        while (nodes.hasNext()) {
            Node node = nodes.nextNode();
            //We are dirty if we are inside an edge folder node and have no no-metada siblings
            Node parent = node.getParent();
            if (parent.isNodeType(ArtifactoryJcrConstants.NT_ARTIFACTORY_FOLDER)) {
                //Are we an edge folder and there are no real artifacts around the metadata file
                boolean fits = true;
                NodeIterator children = parent.getNodes();
                Collection<Node> childrenList = new ArrayList<Node>();
                while (children.hasNext()) {
                    Node child = children.nextNode();
                    childrenList.add(child);
                    String childNodeName = child.getName();
                    NodeType childNodeType = child.getPrimaryNodeType();
                    if (childNodeType.isNodeType(ArtifactoryJcrConstants.NT_ARTIFACTORY_FOLDER) ||
                            !MavenUtils.isMetaData(childNodeName)) {
                        fits = false;
                        break;
                    }
                }
                if (fits) {
                    results.addAll(childrenList);
                }
            }
            //Delete all the found artifacts
            for (Node result : results) {
                Node resultParent = result.getParent();
                NodeLocker nodeLocker = new NodeLocker(resultParent);
                try {
                    nodeLocker.lock();
                    result.remove();
                } finally {
                    nodeLocker.unlock();
                }
            }
        }
        return null;
    }
}
