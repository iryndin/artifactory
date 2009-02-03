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
package org.jfrog.maven.viewer.ui.command;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;

import org.jfrog.maven.viewer.common.Config;
import org.jfrog.maven.viewer.ui.controller.ArtifactGraphVisitor;
import org.jfrog.maven.viewer.domain.Artifact;
import org.jfrog.maven.viewer.domain.ArtifactFactory;
import org.jfrog.maven.viewer.domain.ArtifactVisitor;
import org.jfrog.maven.viewer.ui.event.NewGraphEvent;
import org.jfrog.maven.viewer.ui.model.jung.JungGraph;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.richclient.command.ActionCommand;
import org.springframework.richclient.command.support.ApplicationWindowAwareCommand;
import org.springframework.richclient.command.support.ShowViewCommand;
import org.springframework.richclient.application.ViewDescriptor;
import org.springframework.richclient.application.ApplicationWindow;

/**
 * User: Dror Bereznitsky
 * Date: 10/03/2007
 * Time: 17:41:19
 */
public abstract class CreateGraphCommand extends ActionCommand implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private ArtifactFactory artifactFactory;

    protected CreateGraphCommand(String id) {
        super(id);
    }

    protected void createGraph(Artifact artifact) {
        Graph graph = new DirectedSparseGraph();

        JungGraph jungGraph = new JungGraph(graph);
        ArtifactVisitor visiotr = new ArtifactGraphVisitor(jungGraph, artifactFactory, Config.getTransitiveDepth());
        artifact.accept(visiotr);
        applicationContext.getParent().publishEvent(new NewGraphEvent(this, graph));
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public ArtifactFactory getArtifactFactory() {
        return artifactFactory;
    }

    public void setArtifactFactory(ArtifactFactory artifactFactory) {
        this.artifactFactory = artifactFactory;
    }
}
