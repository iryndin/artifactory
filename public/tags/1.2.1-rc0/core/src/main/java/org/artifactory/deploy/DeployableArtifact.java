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
import org.artifactory.resource.PackagingType;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class DeployableArtifact implements Serializable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(DeployableArtifact.class);

    private static final PackagingType DEFAULT_PACKAGING = PackagingType.jar;

    private String group;
    private String artifact;
    private String version;
    private String classifier;
    private PackagingType packaging = DEFAULT_PACKAGING;
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
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public void update(File file) throws IOException {
        if (file.getName().endsWith(PackagingType.jar.name())) {
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
                    setPackaging(PackagingType.jar);
                    return;
                }
            }
        } else {
            String fileName = file.getName();
            if (fileName.endsWith(PackagingType.pom.name()) || fileName.endsWith(".xml")) {
                try {
                    readModel(new FileInputStream(file));
                } catch (Exception e) {
                    throw new IOException(
                            "Failed to read maven model from '" + file.getName()
                                    + "'. Cause: " + e.getMessage() + ".");
                }
                setPackaging(PackagingType.pom);
            }
        }
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = StringUtils.trim(group);
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = StringUtils.trim(artifact);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = StringUtils.trim(version);
    }

    public String getClassifier() {
        return classifier;
    }


    public void setClassifier(String classifier) {
        this.classifier = StringUtils.trim(classifier);
    }

    public PackagingType getPackaging() {
        return packaging;
    }

    public void setPackaging(PackagingType packaging) {
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
        return StringUtils.isNotEmpty(group);
    }

    public boolean hasArtifact() {
        return StringUtils.isNotEmpty(artifact);
    }

    public boolean hasVersion() {
        return StringUtils.isNotEmpty(version);
    }

    public boolean hasClassifer() {
        return StringUtils.isNotEmpty(classifier);
    }

    public boolean isValid() {
        return hasGroup() && hasArtifact() && hasVersion();
    }

    private void readModel(InputStream is) throws IOException, XmlPullParserException {
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
