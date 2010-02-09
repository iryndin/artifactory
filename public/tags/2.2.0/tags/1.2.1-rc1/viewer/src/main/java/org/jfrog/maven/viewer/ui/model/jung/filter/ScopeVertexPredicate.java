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
package org.jfrog.maven.viewer.ui.model.jung.filter;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.predicates.VertexPredicate;

import java.util.Set;

/**
 * User: Dror Bereznitsky
 * Date: 24/03/2007
 * Time: 01:15:11
 */
public class ScopeVertexPredicate extends VertexPredicate {
    private final ScopeEdgePredicate edgePred;

    public ScopeVertexPredicate(String scope) {
        this.edgePred = new ScopeEdgePredicate(scope);
    }

    public boolean evaluateVertex(ArchetypeVertex v) {
        Set<ArchetypeEdge> edges = v.getIncidentEdges();
        int scopeEdges = 0;
        int nonScopeEdge = 0;
        for (ArchetypeEdge edge : edges) {
            if (edge instanceof DirectedSparseEdge) {
                DirectedSparseEdge directedEdge = (DirectedSparseEdge) edge;
                if (v.equals(directedEdge.getDest())) {
                    if (!edgePred.evaluateEdge(directedEdge)) {
                        scopeEdges++;
                    } else {
                        nonScopeEdge++;
                    }
                }
            }
        }
        return (scopeEdges == 0 || nonScopeEdge > 0);
    }

    public String getScope() {
        return edgePred.getScope();
    }
}
