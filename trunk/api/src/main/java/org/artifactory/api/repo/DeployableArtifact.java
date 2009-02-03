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
package org.artifactory.api.repo;

import org.artifactory.api.mime.ContentType;
import org.artifactory.utils.PathUtils;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class DeployableArtifact implements Serializable {
    private static final String DEFAULT_PACKAGING = ContentType.javaArchive.getDefaultExtension();

    private String group;
    private String artifact;
    private String version;
    private String classifier;
    private String packaging = DEFAULT_PACKAGING;
    /**
     * String representation of the pom.xml The maven Model class is not used because it isn't Serializable.
     */
    private String pomAsString;

    public DeployableArtifact() {
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = PathUtils.trimWhitespace(group);
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = PathUtils.trimWhitespace(artifact);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = PathUtils.trimWhitespace(version);
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = PathUtils.trimWhitespace(classifier);
    }

    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    /**
     * @return The artifact's pom as a String. Might be null.
     */
    public String getPomAsString() {
        return pomAsString;
    }

    public void setPomAsString(String pomAsString) {
        this.pomAsString = pomAsString;
    }

    public void invalidate() {
        group = null;
        artifact = null;
        version = null;
        classifier = null;
        packaging = DEFAULT_PACKAGING;
    }

    public boolean hasGroup() {
        return PathUtils.hasText(group);
    }

    public boolean hasArtifact() {
        return PathUtils.hasText(artifact);
    }

    public boolean hasVersion() {
        return PathUtils.hasText(version);
    }

    public boolean hasClassifer() {
        return PathUtils.hasText(classifier);
    }

    public boolean isValid() {
        return hasGroup() && hasArtifact() && hasVersion();
    }

    @Override
    public String toString() {
        return "Artifact groupId=" + group +
                " artifactId=" + artifact +
                " version=" + version +
                " classifier=" + classifier +
                " packaging=" + packaging;
    }
}
