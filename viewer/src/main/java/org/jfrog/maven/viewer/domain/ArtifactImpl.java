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
package org.jfrog.maven.viewer.domain;

import org.apache.maven.project.MavenProject;
import org.jfrog.maven.viewer.common.ArtifactIdentifier;

import java.util.List;

/**
 * User: Dror Bereznitsky
 * Date: 28/11/2006
 * Time: 15:42:58
 */
class ArtifactImpl implements Artifact {
    private MavenProject model;
    private ArtifactIdentifier dependent;
    private List<ArtifactDependency> dependencies;
    private String scope;

    public ArtifactImpl(MavenProject model) {
        this.model = model;
    }

    public ArtifactImpl(MavenProject model, ArtifactIdentifier dependent) {
        this(model);
        this.dependent = dependent;
        this.scope = "compile";
    }

    public ArtifactImpl(MavenProject model, ArtifactIdentifier dependent, String scope) {
        this(model, dependent);
        this.scope = scope;
    }

    public void accept(ArtifactVisitor visitor) {
        visitor.visitArtifact(this);
        visitor.visitDependencies(this);
    }

    public List<ArtifactDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<ArtifactDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public ArtifactIdentifier getDependent() {
        return dependent;
    }

    public boolean hasDependent() {
        return (dependent != null);
    }

    public void setDependent(ArtifactIdentifier dependent) {
        this.dependent = dependent;
    }

    public ArtifactIdentifier getIdentifier() {
        return new ArtifactIdentifier(model);
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public MavenProject getMavenProject() {
        return model;
    }

    private List<String> getModules() {
        return model.getModules();
    }

}
