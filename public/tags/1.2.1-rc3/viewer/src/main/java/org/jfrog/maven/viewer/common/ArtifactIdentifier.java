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
package org.jfrog.maven.viewer.common;

import org.apache.commons.lang.NullArgumentException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

/**
 * User: Dror Bereznitsky
 * Date: 03/11/2006
 * Time: 23:09:01
 */
public class ArtifactIdentifier {
    private final String artifactId;
    private final String groupId;
    private final String version;
    private String toString;

    public ArtifactIdentifier(String artifactId, String groupId, String version) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.version = version;
        createStringRepresentation();
    }

    public ArtifactIdentifier(Model model) {
        if (model == null) throw new NullArgumentException("domain must not be null");

        this.artifactId = model.getArtifactId();
        this.groupId = model.getGroupId();
        this.version = model.getVersion();
        createStringRepresentation();
    }

    public ArtifactIdentifier(MavenProject mavenProject) {
        if (mavenProject == null) throw new NullArgumentException("mavenProject must not be null");

        this.artifactId = mavenProject.getArtifactId();
        this.groupId = mavenProject.getGroupId();
        this.version = mavenProject.getVersion();
        createStringRepresentation();
    }

    public ArtifactIdentifier(Dependency dep) {
        this.artifactId = dep.getArtifactId();
        this.groupId = dep.getGroupId();
        this.version = dep.getVersion();
        createStringRepresentation();
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getVersion() {
        return version;
    }

    private void createStringRepresentation() {
        this.toString = getGroupId() + ':' + getArtifactId() + ':' + getVersion();
    }

    @Override
    public String toString() {
        return toString;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArtifactIdentifier) {
            ArtifactIdentifier tmp = (ArtifactIdentifier) obj;
            return (
                    groupId.equals(tmp.groupId) &&
                            artifactId.equals(tmp.artifactId) &&
                            version.equals(tmp.version)
            );
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
    	return super.hashCode();
    }
}
