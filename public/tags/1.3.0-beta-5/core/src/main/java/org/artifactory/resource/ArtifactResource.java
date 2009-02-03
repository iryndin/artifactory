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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.Artifact;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.sonatype.nexus.index.context.IndexingContext;

import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactResource extends SimpleRepoResource {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactResource.class);

    private String groupId;
    private String artifactId;
    private String version;
    private String classifier;
    private String type;
    public static final String NEXUS_MAVEN_REPOSITORY_INDEX_ZIP =
            IndexingContext.INDEX_FILE + ".zip";
    public static final String NEXUS_MAVEN_REPOSITORY_INDEX_PROPERTIES =
            IndexingContext.INDEX_FILE + ".properties";

    public ArtifactResource(
            String groupId, String artifactId, String version, String packagingType,
            String classifier, LocalRepo repo) {
        super(repo, getPath(groupId, artifactId, version, classifier, packagingType));
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = packagingType;
        this.classifier = classifier;
    }

    public ArtifactResource(RealRepo repo, String path) {
        super(repo, path);
        init();
    }

    public ArtifactResource(FileInfo fileInfo) {
        super(fileInfo);
        init();
    }

    private void init() {
        groupId = artifactId = version = type = classifier = NA;
        String name = getName();
        if (NEXUS_MAVEN_REPOSITORY_INDEX_ZIP.equals(name) ||
                NEXUS_MAVEN_REPOSITORY_INDEX_PROPERTIES.equals(name)) {
            //Skip index resources
            return;
        }
        String path = getPath();
        //The format of the relative path in maven is a/b/c/artifactId/version where
        //groupId="a.b.c". We split the path to elements and analyze the needed fields.
        LinkedList<String> pathElements = new LinkedList<String>();
        StringTokenizer tokenizer = new StringTokenizer(path, "/");
        while (tokenizer.hasMoreTokens()) {
            pathElements.add(tokenizer.nextToken());
        }
        boolean metaData = MavenUtils.isMetadata(name);
        boolean checksum = MavenUtils.isChecksum(name);
        //Sanity check, we need groupId, artifactId and version
        if (pathElements.size() < 3) {
            setError("The groupId, artifactId and version are unreadable.");
            return;
        }
        //Extract the version, artifactId and groupId
        int pos = pathElements.size() - 2;
        version = pathElements.get(pos--);
        artifactId = pathElements.get(pos--);
        StringBuffer groupIdBuff = new StringBuffer();
        for (; pos >= 0; pos--) {
            if (groupIdBuff.length() != 0) {
                groupIdBuff.insert(0, '.');
            }
            groupIdBuff.insert(0, pathElements.get(pos));
        }
        groupId = groupIdBuff.toString();
        //Extract the type and classifier except for hashes and metadata files
        if (!metaData && !checksum) {
            boolean snapshot = MavenUtils.isVersionSnapshot(version);
            //Extract the type
            String versionInName = version;
            if (snapshot) {
                //For uniqueVersion snapshots extract the version pattern for calulating the type
                Matcher m = MavenUtils.UNIQUE_SNAPSHOT_NAME_PATTERN.matcher(name);
                if (m.matches()) {
                    versionInName = m.group(2);
                }
            }
            int versionEndIdx = name.lastIndexOf(versionInName) + versionInName.length();
            int typeDotStartIdx = name.indexOf('.', versionEndIdx);
            type = name.substring(typeDotStartIdx + 1);
            //Extract the classifier as the delta between the actual name and the base name:
            //[artifactId]-[version]-[classifier].[type]
            if (versionEndIdx < typeDotStartIdx) {
                classifier = name.substring(
                        versionEndIdx + 1, //After the '-'
                        typeDotStartIdx);//To the .[type]
            }
        }
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        if (NA.equals(classifier)) {
            return null;
        }
        return classifier;
    }

    public String getType() {
        return type;
    }

    public boolean isSnapshot() {
        return MavenUtils.isVersionSnapshot(version);
    }

    //TODO: [by yl] This doesn't belong here...
    public String getActualArtifactXml() {
        return (NA.equals(groupId) ? "" : wrapInTag(groupId, "groupId") + "\n") +
                (NA.equals(artifactId) ? "" : wrapInTag(artifactId, "artifactId") + "\n") +
                (NA.equals(version) ? "" : wrapInTag(version, "version") + "\n") +
                (NA.equals(classifier) || classifier == null ?
                        "" : wrapInTag(classifier, "classifier") + "\n");
    }

    public boolean isValid() {
        return !NA.equals(groupId) && !NA.equals(artifactId) &&
                !NA.equals(version) && !NA.equals(type);
    }

    public static String getPath(Artifact artifact) {
        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        String version = artifact.getVersion();
        String classifier = artifact.getClassifier();
        String type = artifact.getType();
        return getPath(groupId, artifactId, version, classifier, type);
    }

    public static String getPath(
            String groupId, String artifactId, String version, String classifier, String type) {
        String path = groupId.replace('.', '/') +
                "/" + artifactId + "/" + version + "/" +
                artifactId + "-" + version +
                (StringUtils.isNotEmpty(classifier) && !NA.equals(classifier) ?
                        "-" + classifier : "") + "." + type;
        return path;
    }

    private static String wrapInTag(String content, String tag) {
        return "<" + tag + ">" + content + "</" + tag + ">";
    }

    private void setError(String message) {
        LOGGER.warn("Failed to build a ArtifactResource from '" + getPath() + "'. " +
                message + ".");
    }

    public MavenArtifactInfo getMavenArtifactInfo() {
        return new MavenArtifactInfo(
                getGroupId(),
                getArtifactId(),
                getVersion(),
                getClassifier(),
                getType()
        );
    }
}
