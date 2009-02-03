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
package org.artifactory.webapp.wicket.panel;

import org.artifactory.repo.LocalRepo;
import org.artifactory.resource.ArtifactResource;
import wicket.behavior.AttributeAppender;
import wicket.markup.html.basic.Label;
import wicket.model.IModel;
import wicket.model.Model;
import wicket.model.PropertyModel;

/**
 * @author Yoav Aharoni
 */
public abstract class WindowPanel extends ArtifactoryPanel {
    private static final String WINDOW_TITLE_KEY = "window.title";

    {
        final Label titleLabel = new Label("title", new PropertyModel(this, "title"));
        final AttributeAppender classAttributeAppender =
                new AttributeAppender("class", new Model("win_wrapper"), " ");
        add(titleLabel);
        add(classAttributeAppender);
    }

    public WindowPanel(String id) {
        super(id);
    }

    public WindowPanel(String id, IModel iModel) {
        super(id, iModel);
    }

    public String getTitle() {
        return getLocalizer().getString(WINDOW_TITLE_KEY, this);
    }

    protected String getArtifactMetadataContent(ArtifactResource pa) {
        String repositoryKey = pa.getRepoKey();
        LocalRepo repo = getCc().localOrCachedRepositoryByKey(repositoryKey);
        String pom = repo.getPomContent(pa);
        if (pom == null) {
            pom = "No POM file found for '" + pa.getName() + "'.";
        }
        String artifactMetadata = pa.getActualArtifactXml();
        StringBuilder result = new StringBuilder();
        if (artifactMetadata != null && artifactMetadata.trim().length() > 0) {
            result.append("------ ARTIFACT EFFECTIVE METADATA BEGIN ------\n")
                    .append(artifactMetadata)
                    .append("------- ARTIFACT EFFECTIVE METADATA END -------\n\n");
        }
        result.append(pom);
        return result.toString();
    }

}
