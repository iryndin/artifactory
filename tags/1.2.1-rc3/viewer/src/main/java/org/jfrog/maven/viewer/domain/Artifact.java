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
 * Date: 29/11/2006
 * Time: 10:25:46
 */
public interface Artifact {
    void accept(ArtifactVisitor visitor);

    boolean hasDependent();

    void setDependent(ArtifactIdentifier dependent);

    ArtifactIdentifier getDependent();

    ArtifactIdentifier getIdentifier();

    String getScope();

    void setScope(String scope);

    MavenProject getMavenProject();

    List<ArtifactDependency> getDependencies();

    void setDependencies(List<ArtifactDependency> dependencies);
}