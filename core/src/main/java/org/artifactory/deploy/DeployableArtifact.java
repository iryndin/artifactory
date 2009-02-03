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
package org.artifactory.deploy;

import org.apache.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.artifactory.maven.MavenUtils;
import org.artifactory.resource.PackagingType;
import org.artifactory.utils.MimeTypes;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class DeployableArtifact implements Serializable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(DeployableArtifact.class);

    private static final String DEFAULT_PACKAGING = PackagingType.jar.name();

    private String group;
    private String artifact;
    private String version;
    private String classifier;
    private String packaging = DEFAULT_PACKAGING;
    private transient Model model;

    public DeployableArtifact() {
    }

    public DeployableArtifact(File file) throws IOException {
        update(file);
    }

    /**
     * Update the artifact model from a jar or pom file
     *
     * @param file .jar or .pom file
     * @throws IOException
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored", "OverlyComplexMethod"})
    public void update(File file) throws IOException {
        String fileName = file.getName();
        if (StringUtils.endsWithIgnoreCase(fileName, PackagingType.jar.name())) {
            //JAR
            JarInputStream jis = new JarInputStream(new FileInputStream(file));
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                //Look for pom.xml in META-INF/maven/
                String name = entry.getName();
                if (name.startsWith("META-INF/maven/") && name.endsWith("pom.xml")) {
                    int size = (int) entry.getSize();
                    //Sanity check
                    if (size < 0) {
                        return;
                    }
                    //Read the uncompressed content
                    try {
                        readModel(jis);
                    } catch (Exception e) {
                        throw new IOException(
                                "Failed to read maven model from '" + entry.getName()
                                        + "'. Cause: " + e.getMessage() + ".");
                    }
                    setPackaging(PackagingType.jar.name());
                    return;
                }
            }
        } else if (StringUtils.endsWithIgnoreCase(fileName, PackagingType.pom.name())
                || StringUtils.endsWithIgnoreCase(fileName, ".xml")) {
            //POM
            try {
                readModel(new FileInputStream(file));
                setPackaging(PackagingType.pom.name());
            } catch (Exception e) {
                //Ignore exception - not every xml is a pom
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Failed to read maven model from '" + fileName + "'. Cause: " +
                            e.getMessage() + ".");
                }
                setPackaging("xml");
            }

        } else {
            //Other extension
            String extension = MimeTypes.getExtension(fileName);
            if (extension != null) {
                setPackaging(extension.substring(1));
            }
        }
        //Calculate the classifier according to the version in the file name
        boolean classifierSet = false;
        if (hasVersion()) {
            int versionBeginIdx = fileName.lastIndexOf(version);
            int classifierBeginIdx = versionBeginIdx + version.length();
            int extBeginIdx = fileName.lastIndexOf('.');
            if (versionBeginIdx > 0 && classifierBeginIdx < extBeginIdx &&
                    fileName.charAt(classifierBeginIdx) == '-') {
                String classif = fileName.substring(classifierBeginIdx + 1, extBeginIdx);
                setClassifier(classif);
            }
            classifierSet = true;
        }
        //Try to guess the artifactId and version properties from the uploadedFile name by regexp
        Matcher matcher = MavenUtils.artifactMatcher(fileName);
        if (matcher.matches()) {
            if (!hasClassifer() && !classifierSet) {
                setClassifier(matcher.group(5));
            }
            if (!hasArtifact()) {
                setArtifact(matcher.group(1));
            }
            if (!hasVersion()) {
                setVersion(matcher.group(2));
            }
        }
        //Complete values by falling back to dumb defaults
        if (!StringUtils.hasText(getArtifact())) {
            setArtifact(fileName);
        }
        if (!StringUtils.hasText(getGroup())) {
            //If we have no group, set it to be the same as the artifact name
            setGroup(getArtifact());
        }
        if (!StringUtils.hasText(getVersion())) {
            setVersion(fileName);
        }
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = StringUtils.trimWhitespace(group);
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = StringUtils.trimWhitespace(artifact);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = StringUtils.trimWhitespace(version);
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = StringUtils.trimWhitespace(classifier);
    }

    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public Model getModel() {
        return model;
    }

    public void invalidate() {
        group = null;
        artifact = null;
        version = null;
        classifier = null;
        packaging = DEFAULT_PACKAGING;
        model = null;
    }

    public boolean hasGroup() {
        return StringUtils.hasText(group);
    }

    public boolean hasArtifact() {
        return StringUtils.hasText(artifact);
    }

    public boolean hasVersion() {
        return StringUtils.hasText(version);
    }

    public boolean hasClassifer() {
        return StringUtils.hasText(classifier);
    }

    public boolean isValid() {
        return hasGroup() && hasArtifact() && hasVersion();
    }

    @SuppressWarnings({"ConstantConditions"})
    private void readModel(InputStream is)
            throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        model = reader.read(new InputStreamReader(is, "utf-8"));
        Parent parent = model.getParent();
        String groupId = model.getGroupId();
        if (groupId == null && parent != null) {
            groupId = parent.getGroupId();
        }
        setGroup(groupId);
        setArtifact(model.getArtifactId());
        String version = model.getVersion();
        if (version == null && parent != null) {
            version = parent.getVersion();
        }
        setVersion(version);
    }
}
