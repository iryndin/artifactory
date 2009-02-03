/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jfrog.maven.viewer.ui.model.jung;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseVertex;
import edu.uci.ics.jung.utils.UserData;

import org.apache.log4j.Logger;
import org.jfrog.maven.viewer.common.ArtifactIdentifier;
import org.jfrog.maven.viewer.common.JungHelper;
import org.jfrog.maven.viewer.ui.model.Graph;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Dror Bereznitsky
 * Date: 05/01/2007
 * Time: 00:02:34
 */
public class JungGraph implements Graph {
    private edu.uci.ics.jung.graph.Graph graph;
    private Map<String, Vertex> artifactVertex = new HashMap<String, Vertex>();

    private final static Logger logger = Logger.getLogger(JungGraph.class);

    public JungGraph(edu.uci.ics.jung.graph.Graph graph) {
        this.graph = graph;
    }

    public void addAtrifactVertex(ArtifactIdentifier artifactIdentifier, int depth) {
        if (vertexExists(artifactIdentifier)) return;
        Vertex v = new DirectedSparseVertex();
        graph.addVertex(v);
        try {
            JungHelper.getStringLabeller(graph).setLabel(v, artifactIdentifier.toString());
        } catch (StringLabeller.UniqueLabelException e) {
            logger.error(e);
        }

        v.setUserDatum("identifier", artifactIdentifier, UserData.SHARED);

        v.setUserDatum("depth", depth, UserData.SHARED);

        artifactVertex.put(artifactIdentifier.toString(), v);
    }

    public void addMissingArtifactVertex(ArtifactIdentifier artifactIdentifier, int depth) {
        addAtrifactVertex(artifactIdentifier, depth);
        findVertex(artifactIdentifier).setUserDatum("dummy", Boolean.TRUE, UserData.SHARED);
    }

    public void createEdge(ArtifactIdentifier dependent, ArtifactIdentifier dependency, String scope) {
        Vertex dependentV = findVertex(dependent);
        Vertex dependencyV = findVertex(dependency);
        Edge edge = new DirectedSparseEdge(dependentV, dependencyV);
        edge.addUserDatum("label", scope, UserData.SHARED);
        edge.addUserDatum("tooltip", scope, UserData.SHARED);
        try {
            graph.addEdge(edge);
        } catch (Exception e) {
            logger.error(
                    MessageFormat.format("Could not create edge between {0} and {1}: {2}", dependentV, dependencyV, e.getMessage()));
        }
    }

    public edu.uci.ics.jung.graph.Graph getGraph() {
        return graph;
    }

    private Vertex findVertex(ArtifactIdentifier artifactIdentifier) {
        return artifactVertex.get(artifactIdentifier.toString());
    }

    private boolean vertexExists(ArtifactIdentifier artifactIdentifier) {
        return artifactVertex.containsKey(artifactIdentifier.toString());
    }
}
