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
package org.jfrog.maven.viewer.ui.controller;

import org.apache.log4j.Logger;
import org.jfrog.maven.viewer.common.ArtifactIdentifier;
import org.jfrog.maven.viewer.domain.Artifact;
import org.jfrog.maven.viewer.domain.ArtifactDependency;
import org.jfrog.maven.viewer.domain.ArtifactFactory;
import org.jfrog.maven.viewer.domain.ArtifactVisitor;
import org.jfrog.maven.viewer.ui.model.Graph;

import java.text.MessageFormat;
import java.util.List;

/**
 * User: Dror Bereznitsky
 * Date: 28/11/2006
 * Time: 15:52:37
 */
public class ArtifactGraphVisitor implements ArtifactVisitor {

    private Graph graph;

    private ArtifactFactory artifactFactory;

    private final int depth;
    private int currentDepth;

    private static final Logger logger = Logger.getLogger(ArtifactGraphVisitor.class);

    public ArtifactGraphVisitor(Graph graph, ArtifactFactory artifactFactory) {
        this.graph = graph;
        this.artifactFactory = artifactFactory;
        this.depth = -1;
        this.currentDepth = 0;
    }

    public ArtifactGraphVisitor(Graph graph, ArtifactFactory artifactFactory, int depth) {
        this.graph = graph;
        this.artifactFactory = artifactFactory;
        this.depth = depth;
        this.currentDepth = 0;
    }

    public void visitArtifact(Artifact artifact) {
        if (!isDepthAllowed()) return;

        ArtifactIdentifier id = artifact.getIdentifier();

        if (artifact.getMavenProject() != null) {
            graph.addAtrifactVertex(id, currentDepth);
        } else {
            graph.addMissingArtifactVertex(id, currentDepth);
        }
        if (artifact.hasDependent()) {
            graph.createEdge(artifact.getDependent(), id, artifact.getScope());
        }
    }

    public void visitDependencies(Artifact dependent) {
        increaseDepth();
        if (!isDepthAllowed()) {
            decreaseDepth();
            logger.debug(
                    MessageFormat.format("Maximal depth reached, will not visit dependencies of {0}", dependent.getIdentifier()));
            return;
        }
        logger.debug(
                MessageFormat.format("Visiting {0}dependencies, current depth = {1}", dependent.getIdentifier(), currentDepth));
        for (ArtifactDependency dependency : dependent.getDependencies()) {
            Artifact artifact = artifactFactory.createArtifact(dependency, dependent);
            artifact.accept(this);
        }
        decreaseDepth();
    }

    private void increaseDepth() {
        currentDepth++;
    }

    private void decreaseDepth() {
        currentDepth--;
    }

    private boolean isDepthAllowed() {
        return (depth == -1 || currentDepth <= depth);
    }
}
