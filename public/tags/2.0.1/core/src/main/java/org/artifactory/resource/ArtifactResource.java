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
package org.artifactory.resource;

import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.sonatype.nexus.index.context.IndexingContext;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactResource extends FileResource {
    public static final String NEXUS_MAVEN_REPOSITORY_INDEX_ZIP = IndexingContext.INDEX_FILE + ".zip";
    public static final String NEXUS_MAVEN_REPOSITORY_INDEX_PROPERTIES =
            IndexingContext.INDEX_FILE + ".properties";

    private final MavenArtifactInfo mavenInfo;

    public ArtifactResource(FileInfo fileInfo) {
        super(fileInfo);
        String name = getInfo().getName();
        if (!NEXUS_MAVEN_REPOSITORY_INDEX_ZIP.equals(name) &&
                !NEXUS_MAVEN_REPOSITORY_INDEX_PROPERTIES.equals(name)) {
            mavenInfo = MavenArtifactInfo.fromRepoPath(getRepoPath());
        } else {
            mavenInfo = new MavenArtifactInfo();
        }
    }

    public MavenArtifactInfo getMavenInfo() {
        return mavenInfo;
    }
}
