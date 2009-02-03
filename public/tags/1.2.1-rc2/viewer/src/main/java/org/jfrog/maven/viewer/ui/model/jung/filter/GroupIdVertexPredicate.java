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

import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.predicates.VertexPredicate;
import org.jfrog.maven.viewer.common.ArtifactIdentifier;
import org.jfrog.maven.viewer.ui.model.jung.JungGraph;

/**
 * Created by IntelliJ IDEA.
 * User: Dror Bereznitsky
 * Date: May 4, 2007
 * Time: 6:27:15 PM
 */
public class GroupIdVertexPredicate extends VertexPredicate {
    private String groupId;

    public GroupIdVertexPredicate(String groupId) {
        this.groupId = groupId;
    }

    public boolean evaluateVertex(ArchetypeVertex v) {
        final ArtifactIdentifier ai =
                (ArtifactIdentifier) v.getUserDatum(JungGraph.ARTIFACT_IDENTIFIER);
        return (groupId.equals(ai.getGroupId()));
    }
}
