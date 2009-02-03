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

import org.jfrog.maven.viewer.common.ArtifactIdentifier;
import org.jfrog.maven.viewer.domain.Artifact;
import org.jfrog.maven.viewer.domain.exception.ArtifactCreationException;
import org.jfrog.maven.viewer.ui.view.dialog.GraphIdDialog;
import org.springframework.richclient.application.ApplicationException;

/**
 * User: Dror Bereznitsky
 * Date: 15/03/2007
 * Time: 19:21:52
 */
public class CreateGraphByIdCommand extends CreateGraphCommand {

    public CreateGraphByIdCommand() {
        super("createGraphById");
    }

    protected void doExecuteCommand() {
        GraphIdDialog dialog = new GraphIdDialog();
        dialog.showDialog();

        if (dialog.isCancled()) return;

        ArtifactIdentifier artifactIdentifier = dialog.getArtifactIdentifier();
        try {
            Artifact artifact = getArtifactFactory().createArtifact(artifactIdentifier);
            createGraph(artifact);
        } catch (ArtifactCreationException e) {
            throw new ApplicationException("Could not process POM identified by: " + artifactIdentifier, e);
        }
    }
}
