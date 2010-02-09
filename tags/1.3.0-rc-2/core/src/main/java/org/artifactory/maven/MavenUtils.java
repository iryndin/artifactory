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
package org.artifactory.maven;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.artifact.transform.SnapshotTransformation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.repo.exception.maven.BadPomException;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantsValue;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.util.PathUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.*;
import java.text.ParseException;
import java.util.Date;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class MavenUtils {
    private static final Logger log = LoggerFactory.getLogger(MavenUtils.class);

    public static void validatePomTargetPath(InputStream in, String relPath) throws IOException, BadPomException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            Model model = reader.read(new InputStreamReader(in, "utf-8"));
            String groupId = model.getGroupId();
            if (StringUtils.hasLength(groupId)) {
                //Do not verify if the pom's groupid does not exist (inherited)
                String modelVersion = model.getVersion();
                //Version may come from the parent
                if (!StringUtils.hasLength(modelVersion)) {
                    Parent parent = model.getParent();
                    if (parent != null) {
                        modelVersion = parent.getVersion();
                    }
                }
                //For snapshots with unique snapshot version, do not include the model version in the path
                boolean snapshot = MavenNaming.isSnapshot(relPath);
                boolean versionSnapshot = MavenNaming.isVersionSnapshot(modelVersion);
                String pathPrefix = null;
                if (snapshot && !versionSnapshot) {
                    pathPrefix = groupId.replace('.', '/') + "/" + model.getArtifactId() + "/";
                } else if (StringUtils.hasLength(modelVersion)) {
                    pathPrefix = groupId.replace('.', '/') + "/" + model.getArtifactId() + "/" + modelVersion;
                }
                //Do not validate paths that contain property references
                if (pathPrefix != null && !pathPrefix.contains("${")
                        && !StringUtils.startsWithIgnoreCase(relPath, pathPrefix)) {
                    final String msg = "The target deployment path '" + relPath +
                            "' does not match the POM's expected path prefix '" + pathPrefix +
                            "'. Please verify your POM content for correctness and make sure the source path is a " +
                            "valid Maven 2 repository root path.";
                    if (ConstantsValue.suppressPomConsistencyChecks.getBoolean()) {
                        log.error(msg + " POM consistency checks are suppressed. Broken artifacts might have been " +
                                "stored in the repository - please resolve this manually.");
                    } else {
                        throw new BadPomException(msg + " Some artifacts might have been incorrectly imported - " +
                                "please remove them manually.");
                    }
                }
            }
        } catch (XmlPullParserException e) {
            throw new BadPomException("Failed to read POM for '" + relPath + "'.");
        }
    }

    public static String dateToTimestamp(Date date) {
        return SnapshotTransformation.getUtcDateFormatter().format(date);
    }

    public static Date timestampToDate(String timestamp) {
        try {
            return SnapshotTransformation.getUtcDateFormatter().parse(timestamp);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to transfor timestamp to date.", e);
        }
    }

    public static String getArtifactMetadataContent(ArtifactResource pa) {
        String repositoryKey = pa.getRepoPath().getRepoKey();
        InternalRepositoryService repositoryService =
                (InternalRepositoryService) ContextHelper.get().getRepositoryService();
        LocalRepo repo = repositoryService.localOrCachedRepositoryByKey(repositoryKey);
        String pom = repo.getPomContent(pa);
        if (pom == null) {
            pom = "No POM file found for '" + pa.getRepoPath().getName() + "'.";
        }
        String artifactMetadata = pa.getMavenInfo().getXml();
        StringBuilder result = new StringBuilder();
        if (artifactMetadata != null && artifactMetadata.trim().length() > 0) {
            result.append("------ ARTIFACT EFFECTIVE METADATA BEGIN ------\n")
                    .append(artifactMetadata)
                    .append("------- ARTIFACT EFFECTIVE METADATA END -------\n\n");
        }
        result.append(pom);
        return result.toString();
    }

    /**
     * Creates a maven <code>Metadata</code> out of a string.
     *
     * @param metadataAsString String representing content of maven-metadata.xml
     * @return Metadata object created from the input string
     * @throws IOException If the input string is not a valid maven metadata
     */
    public static Metadata toMavenMetadata(String metadataAsString) throws IOException {
        return toMavenMetadata(new StringReader(metadataAsString));
    }

    /**
     * Creates a maven <code>Metadata</code> out of an input stream and will close the stream.
     *
     * @param metadataStream An input stream representing content of maven-metadata.xml
     * @return Metadata object created from the input stream
     * @throws IOException If the input stream is not a valid maven metadata
     */
    public static Metadata toMavenMetadata(InputStream metadataStream) throws IOException {
        return toMavenMetadata(new InputStreamReader(metadataStream, "utf-8"));
    }

    /**
     * Creates a maven <code>Metadata</code> out of a <code>java.io.Reader</code> and will close the reader.
     *
     * @param reader Reader representing content of maven-metadata.xml
     * @return Metadata object created from the reader
     * @throws IOException If the input reader doesn't holds a valid maven metadata
     */
    public static Metadata toMavenMetadata(Reader reader) throws IOException {
        MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();
        try {
            return metadataReader.read(reader);
        } catch (XmlPullParserException e) {
            throw new IOException("Failed to parse metadata: " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    /**
     * Create xml string from the input <code>Metadata</code>.
     *
     * @param metadata Maven metadata object
     * @return Xml string for the input metadata
     */
    public static String mavenMetadataToString(Metadata metadata) throws IOException {
        MetadataXpp3Writer writer = new MetadataXpp3Writer();
        StringWriter stringWriter = new StringWriter();
        writer.write(stringWriter, metadata);
        return stringWriter.toString();
    }

    public static Model getMavenModel(MavenArtifactInfo artifactInfo) {
        //Check if we already have a string model for the pom (set from the jar internal pom or during regular pom
        //deployment)
        String originalPomAsString = artifactInfo.getModelAsString();
        //Create the model
        Model model;
        if (originalPomAsString == null) {
            //Build the model based on user provided values
            model = generateDefaultPom(artifactInfo);
        } else {
            //Build the model based on the string and pacth it with user values
            model = stringToMavenModel(originalPomAsString);
            model.setGroupId(artifactInfo.getGroupId());
            model.setArtifactId(artifactInfo.getArtifactId());
            model.setVersion(artifactInfo.getVersion());
        }
        return model;
    }

    public static String mavenModelToString(Model model) {
        MavenXpp3Writer writer = new MavenXpp3Writer();
        StringWriter stringWriter = new StringWriter();
        try {
            writer.write(stringWriter, model);
        } catch (IOException e) {
            throw new RepositoryRuntimeException("Failed to convert maven model to string", e);
        }
        return stringWriter.toString();
    }

    public static Model stringToMavenModel(String pomAsString) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        StringReader pomStream = new StringReader(pomAsString);
        try {
            return reader.read(pomStream);
        } catch (Exception e) {
            throw new RepositoryRuntimeException("Failed to convert string to maven model", e);
        }
    }

    private static Model generateDefaultPom(MavenArtifactInfo artifactInfo) {
        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId(artifactInfo.getGroupId());
        model.setArtifactId(artifactInfo.getArtifactId());
        model.setVersion(artifactInfo.getVersion());
        model.setPackaging(artifactInfo.getType());
        model.setDescription("Artifactory auto generated POM");
        return model;
    }

    @SuppressWarnings({"OverlyComplexMethod"})
    public static MavenArtifactInfo artifactInfoFromFile(File file) {
        MavenArtifactInfo result = new MavenArtifactInfo();
        final ContentType ct = NamingUtils.getContentType(file);
        String fileName = file.getName();
        if (ct.isJarVariant()) {
            //JAR variant
            JarInputStream jis = null;
            try {
                jis = new JarInputStream(new FileInputStream(file));
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    //Look for pom.xml in META-INF/maven/
                    String name = entry.getName();
                    if (name.startsWith("META-INF/maven/") && name.endsWith("pom.xml")) {
                        int size = (int) entry.getSize();
                        //Sanity check
                        if (size <= 0) {
                            log.warn("Found pom.xml file with size {} inside the zip. Ignoring", size);
                            break;
                        }
                        //Read the uncompressed content
                        try {
                            readModel(jis, result);
                        } catch (Exception e) {
                            log.warn("Failed to read maven model from '" + entry.getName()
                                    + "'. Cause: " + e.getMessage() + ".", e);
                            break;
                        }
                        result.setType(MavenArtifactInfo.JAR);
                        // all data is read from the pom so we can return
                        return result;
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to read maven model from '" + file
                        + "'. Cause: " + e.getMessage() + ".", e);
            } finally {
                IOUtils.closeQuietly(jis);
            }
        } else if (ct.isXml()) {
            //POM
            try {
                FileInputStream in = new FileInputStream(file);
                try {
                    readModel(in, result);
                } finally {
                    IOUtils.closeQuietly(in);
                }
                result.setType(MavenArtifactInfo.POM);
            } catch (Exception e) {
                //Ignore exception - not every xml is a pom
                if (log.isDebugEnabled()) {
                    log.debug("Failed to read maven model from '" + fileName + "'. Cause: " + e.getMessage() + ".");
                }
                result.setType(MavenArtifactInfo.XML);
            }
        } else {
            //Other extension
            String extension = PathUtils.getExtension(fileName);
            if (extension != null) {
                result.setType(extension);
            }
        }
        //Calculate the classifier according to the version in the file name
        boolean classifierSet = false;
        if (result.hasVersion()) {
            String version = result.getVersion();
            int versionBeginIdx = fileName.lastIndexOf(version);
            int classifierBeginIdx = versionBeginIdx + version.length();
            int extBeginIdx = fileName.lastIndexOf('.');
            if (versionBeginIdx > 0 && classifierBeginIdx < extBeginIdx &&
                    fileName.charAt(classifierBeginIdx) == '-') {
                String classif = fileName.substring(classifierBeginIdx + 1, extBeginIdx);
                result.setClassifier(classif);
            }
            classifierSet = true;
        }
        //Try to guess the artifactId and version properties from the uploadedFile name by regexp
        Matcher matcher = MavenNaming.artifactMatcher(fileName);
        if (matcher.matches()) {
            if (!result.hasClassifier() && !classifierSet) {
                result.setClassifier(matcher.group(5));
            }
            if (!result.hasArtifactId()) {
                result.setArtifactId(matcher.group(1));
            }
            if (!result.hasVersion()) {
                result.setVersion(matcher.group(2));
            }
        }
        //Complete values by falling back to dumb defaults
        if (MavenArtifactInfo.NA.equals(result.getArtifactId())) {
            result.setArtifactId(fileName);
        }
        if (MavenArtifactInfo.NA.equals(result.getGroupId())) {
            //If we have no group, set it to be the same as the artifact name
            result.setGroupId(result.getArtifactId());
        }
        if (MavenArtifactInfo.NA.equals(result.getVersion())) {
            result.setVersion(fileName);
        }
        return result;
    }

    public static void readModel(InputStream is, MavenArtifactInfo da) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        InputStreamReader pomStream = new InputStreamReader(is, "utf-8");
        Model model = reader.read(pomStream);

        Parent parent = model.getParent();
        String groupId = model.getGroupId();
        if (groupId == null && parent != null) {
            groupId = parent.getGroupId();
        }
        da.setGroupId(groupId);
        da.setArtifactId(model.getArtifactId());
        String version = model.getVersion();
        if (version == null && parent != null) {
            version = parent.getVersion();
        }
        da.setVersion(version);
        String pomAsString = MavenUtils.mavenModelToString(model);
        da.setModelAsString(pomAsString);
    }

    public static File addPomFileMetadata(File uploadedFile, Artifact artifact, String pomAsString) {
        //Create the pom file in the uploads dir
        String pomFileName = uploadedFile.getName() + "." + MavenArtifactInfo.POM;
        //Create the upload folder every time (e.g., in case it has been reaped)
        File pomFile = new File(ArtifactoryHome.getTmpUploadsDir(), pomFileName);
        //Write the pom to the file
        OutputStreamWriter osw = null;
        try {
            FileUtils.forceMkdir(ArtifactoryHome.getTmpUploadsDir());
            osw = new OutputStreamWriter(new FileOutputStream(pomFile), "utf-8");
            IOUtils.write(pomAsString, osw);
        } catch (Exception e) {
            String msg = "Cannot save Pom file " + pomFile.getName() + ". Cause: " + e.getMessage();
            log.debug(msg, e);
            throw new RepositoryRuntimeException(msg, e);
        } finally {
            IOUtils.closeQuietly(osw);
        }
        //Add project metadata that will trigger additional deployment of the pom file
        ArtifactMetadata metadata = new ProjectArtifactMetadata(artifact, pomFile);
        artifact.addMetadata(metadata);
        return pomFile;
    }
}
