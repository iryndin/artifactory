/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.api.maven;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * User: freds Date: Aug 3, 2008 Time: 11:39:07 AM
 */
public class MavenArtifactInfo extends MavenUnitInfo {
    private static final Logger log = LoggerFactory.getLogger(MavenArtifactInfo.class);

    public static final String ROOT = "artifactory-maven-artifact";

    private String classifier;
    private String type;

    private String path;
    private boolean autoCalculatePath = true;
    private boolean builtFromPomInfo;

    /**
     * String representation of the pom.xml The maven Model class is not used because it isn't Serializable.
     */
    private String modelAsString;

    public MavenArtifactInfo() {
        super(NA, NA, NA);
        this.classifier = NA;
        this.type = JAR;
    }

    public MavenArtifactInfo(String groupId, String artifactId, String version, String classifier, String type) {
        super(groupId, artifactId, version);
        if (PathUtils.hasText(classifier)) {
            this.classifier = classifier;
        } else {
            this.classifier = NA;
        }
        if (PathUtils.hasText(type)) {
            this.type = type;
        } else {
            this.type = JAR;
        }
    }

    public MavenArtifactInfo(MavenArtifactInfo copy) {
        super(copy);
        this.classifier = copy.classifier;
        this.type = copy.type;
    }

    public String getClassifier() {
        if (!hasClassifier()) {
            return null;
        }
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public boolean hasClassifier() {
        return classifier != null && !NA.equals(classifier);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean isValid() {
        if (!isAutoCalculatePath()) {
            return !StringUtils.isEmpty(path);
        }
        return super.isValid() && hasVersion();
    }

    @Override
    public String getPath() {
        if (autoCalculatePath) {
            return getMavenPath();
        } else {
            if (path == null) {
                path = getMavenPath();
            }
            return path;
        }
    }

    public void setPath(String path) {
        if (!autoCalculatePath) {
            this.path = path;
        }
    }

    private String getMavenPath() {
        String mavenPath = addPath(new StringBuilder()).toString();
        return mavenPath;
    }

    @Override
    public String getXml() {
        if (!isValid()) {
            return "<error>Invalid Maven Artifact</error>";
        }
        XmlBuilder builder = addBaseTags(new XmlBuilder());
        if (hasClassifier()) {
            builder.wrapInTag(classifier, "classifier");
        }
        if (!JAR.equals(type)) {
            builder.wrapInTag(type, "type");
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return getGroupId() + ":" + getArtifactId() + ":" + getVersion() +
                (classifier != null ? (":" + classifier) : "") + ":" + type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MavenArtifactInfo)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        MavenArtifactInfo info = (MavenArtifactInfo) o;
        return !(classifier != null ? !classifier.equals(info.classifier) : info.classifier != null) &&
                type.equals(info.type);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
        result = 31 * result + type.hashCode();
        return result;
    }

    public static MavenArtifactInfo fromRepoPath(RepoPath repoPath) {
        String groupId, artifactId, version, type = MavenUnitInfo.NA, classifier = MavenUnitInfo.NA;

        String path = repoPath.getPath();
        String name = repoPath.getName();

        //The format of the relative path in maven is a/b/c/artifactId/version/fileName where
        //groupId="a.b.c". We split the path to elements and analyze the needed fields.
        LinkedList<String> pathElements = new LinkedList<String>();
        StringTokenizer tokenizer = new StringTokenizer(path, "/");
        while (tokenizer.hasMoreTokens()) {
            pathElements.add(tokenizer.nextToken());
        }
        boolean metaData = NamingUtils.isMetadata(name);
        boolean checksum = NamingUtils.isChecksum(name);
        //Sanity check, we need groupId, artifactId and version
        if (pathElements.size() < 3) {
            log.debug("Cannot build MavenArtifactInfo from '{}'. The groupId, artifactId and version are unreadable.",
                    repoPath);
            return new MavenArtifactInfo();
        }

        //Extract the version, artifactId and groupId
        int pos = pathElements.size() - 2;  // one before the last path element
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
            boolean snapshot = MavenNaming.isVersionSnapshot(version);
            //Extract the type
            String versionInName = version;
            if (snapshot && MavenNaming.isUniqueSnapshotFileName(name)) {
                //For uniqueVersion snapshots extract the version pattern for calulating the type
                versionInName = MavenNaming.getUniqueSnapshotVersionTimestampAndBuildNumber(name);
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
        return new MavenArtifactInfo(groupId, artifactId, version, classifier, type);
    }

    /**
     * @return The artifact's pom as a String. Might be null.
     */
    public String getModelAsString() {
        return modelAsString;
    }

    public void setModelAsString(String modelAsString) {
        this.modelAsString = modelAsString;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        setClassifier(null);
        setType(JAR);
    }

    protected StringBuilder addPath(StringBuilder path) {
        if (isValid()) {
            addBasePath(path);
            path.append("/").append(getArtifactId()).append("-").append(getVersion());
            if (hasClassifier()) {
                path.append("-").append(classifier);
            }
            path.append(".").append(type);
        }
        return path;
    }


    public boolean isAutoCalculatePath() {
        return autoCalculatePath;
    }

    public void setAutoCalculatePath(boolean autoCalculatePath) {
        this.autoCalculatePath = autoCalculatePath;
        if (!autoCalculatePath) {
            //Initialize path with the latest version.
            // Set dummy path to be valid
            path = "unknown";
            path = getMavenPath();
        }
    }

    public boolean isBuiltFromPomInfo() {
        return builtFromPomInfo;
    }

    public void setBuiltFromPomInfo(boolean builtFromPomInfo) {
        this.builtFromPomInfo = builtFromPomInfo;
    }
}
