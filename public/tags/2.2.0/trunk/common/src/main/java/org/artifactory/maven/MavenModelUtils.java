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

package org.artifactory.maven;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
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
import org.artifactory.ivy.IvyNaming;
import org.artifactory.ivy.IvyService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.PathUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;

import java.io.*;
import java.text.ParseException;
import java.util.Date;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class MavenModelUtils {
    private static final Logger log = LoggerFactory.getLogger(MavenModelUtils.class);

    public static void validatePomTargetPath(InputStream in, String relPath, boolean suppressPomConsistencyChecks)
            throws IOException {
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
                    pathPrefix = groupId.replace('.', '/') + "/" + model.getArtifactId() + "/" +
                            modelVersion;
                }
                //Do not validate paths that contain property references
                if (pathPrefix != null && !pathPrefix.contains("${")
                        && !StringUtils.startsWithIgnoreCase(relPath, pathPrefix)) {
                    final String msg = "The target deployment path '" + relPath +
                            "' does not match the POM's expected path prefix '" + pathPrefix +
                            "'. Please verify your POM content for correctness and make sure the source path is a " +
                            "valid Maven 2 repository root path.";
                    if (suppressPomConsistencyChecks) {
                        log.error(msg +
                                " POM consistency checks are suppressed. Broken artifacts might have been " +
                                "stored in the repository - please resolve this manually.");
                    } else {
                        throw new BadPomException(msg);
                    }
                }
            }
        } catch (XmlPullParserException e) {
            if (log.isDebugEnabled()) {
                try {
                    in.reset();
                    InputStreamReader isr = new InputStreamReader(in, "utf-8");
                    String s = readString(isr);
                    log.debug("Could not parse bad POM for '{}'. Bad POM content:\n{}\n", relPath, s);
                } catch (Exception ex) {
                    log.trace("Could not extract bad POM content for '{}': {}.", relPath, e.getMessage());
                }
            }
            throw new BadPomException("Failed to read POM for '" + relPath + "': " + e.getMessage() + ".");
        }
    }

    public static String dateToTimestamp(Date date) {
        return SnapshotTransformation.getUtcDateFormatter().format(date);
    }

    public static Date timestampToDate(String timestamp) {
        try {
            return SnapshotTransformation.getUtcDateFormatter().parse(timestamp);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to transfer timestamp to date.", e);
        }
    }

    /**
     * Creates a maven <code>Metadata</code> out of a string.
     *
     * @param metadataAsString String representing content of maven-metadata.xml
     * @return Metadata object created from the input string
     * @throws java.io.IOException If the input string is not a valid maven metadata
     */
    public static Metadata toMavenMetadata(String metadataAsString) throws IOException {
        return toMavenMetadata(new StringReader(metadataAsString));
    }

    /**
     * Creates a maven <code>Metadata</code> out of an input stream and will close the stream.
     *
     * @param metadataStream An input stream representing content of maven-metadata.xml
     * @return Metadata object created from the input stream
     * @throws java.io.IOException If the input stream is not a valid maven metadata
     */
    public static Metadata toMavenMetadata(InputStream metadataStream) throws IOException {
        return toMavenMetadata(new InputStreamReader(metadataStream, "utf-8"));
    }

    /**
     * Creates a maven <code>Metadata</code> out of a <code>java.io.Reader</code> and will close the reader.
     *
     * @param reader Reader representing content of maven-metadata.xml
     * @return Metadata object created from the reader
     * @throws java.io.IOException If the input reader doesn't holds a valid maven metadata
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

    static Model generateDefaultPom(MavenArtifactInfo artifactInfo) {
        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId(artifactInfo.getGroupId());
        model.setArtifactId(artifactInfo.getArtifactId());
        model.setVersion(artifactInfo.getVersion());
        model.setPackaging(artifactInfo.getType());
        model.setDescription("Artifactory auto generated POM");
        return model;
    }

    public static void fillArtifactInfoFromPomModel(InputStream is, MavenArtifactInfo artifactInfo)
            throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        InputStreamReader pomStream = new InputStreamReader(is, "utf-8");
        Model model = reader.read(pomStream);

        Parent parent = model.getParent();
        String groupId = model.getGroupId();
        if (groupId == null && parent != null) {
            groupId = parent.getGroupId();
        }
        artifactInfo.setGroupId(groupId);
        artifactInfo.setArtifactId(model.getArtifactId());
        String version = model.getVersion();
        if (version == null && parent != null) {
            version = parent.getVersion();
        }
        artifactInfo.setVersion(version);
        String pomAsString = mavenModelToString(model);
        artifactInfo.setModelAsString(pomAsString);
        artifactInfo.setBuiltFromPomInfo(true);
    }

    /**
     * Returns a maven artifact info after gathering information from the given file
     *
     * @param file File to gather information from
     * @return MavenArtifactInfo object containing gathered information
     */
    public static MavenArtifactInfo artifactInfoFromFile(File file) {
        MavenArtifactInfo result = new MavenArtifactInfo();
        gatherInfoFromFile(file, result);
        gatherInfoFromMatching(file, result);
        gatheringFallback(file, result);
        return result;
    }

    public static File addPomFileMetadata(File uploadedFile, Artifact artifact, String pomAsString,
            File tempUploadsDir) {
        //Create the pom file in the uploads dir
        String pomFileName = uploadedFile.getName() + "." + MavenArtifactInfo.POM;
        //Create the upload folder every time (e.g., in case it has been reaped)
        File pomFile = new File(tempUploadsDir, pomFileName);
        //Write the pom to the file
        OutputStreamWriter osw = null;
        try {
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
        if (artifact != null) {
            ArtifactMetadata metadata = new ProjectArtifactMetadata(artifact, pomFile);
            artifact.addMetadata(metadata);
        }
        return pomFile;
    }

    /**
     * Gathers maven artifact information from the given file in several different methods (to handle each kind of
     * file)
     *
     * @param file   Uploaded file to gather info from
     * @param result MavenArtifactInfo object to append info to
     */
    private static void gatherInfoFromFile(File file, MavenArtifactInfo result) {
        final ContentType ct = NamingUtils.getContentType(file);
        String fileName = file.getName();
        if (ct.isJarVariant()) {
            //File is a jar variant
            result.setType(PathUtils.getExtension(fileName));
            gatherInfoFromJarFile(file, result);
        } else if (MavenNaming.isClientOrServerPom(fileName)) {
            gatherInfoFromPomFile(file, result);
        } else if (IvyNaming.isIvyFileName(fileName)) {
            gatherInfoFromIvyFile(file, result);
        } else {
            //Other extension
            String extension = PathUtils.getExtension(fileName);
            if (extension != null) {
                result.setType(extension);
            }
        }
    }

    /**
     * Gathers maven artifact information which was (or was not) managed to gather from the given Jar file
     *
     * @param file   Jar file to gather info from
     * @param result MavenArtifactInfo object to append info to
     */
    private static void gatherInfoFromJarFile(File file, MavenArtifactInfo result) {
        JarInputStream jis = null;
        JarEntry entry;
        try {
            //Create a stream and try to find the pom file within the jar
            jis = new JarInputStream(new FileInputStream(file));
            entry = getPomFile(jis);

            //If a valid pom file was found
            if (entry != null) {
                try {
                    //Read the uncompressed content
                    fillArtifactInfoFromPomModel(jis, result);
                } catch (Exception e) {
                    log.warn("Failed to read maven model from '" + entry.getName() + "'. Cause: " + e.getMessage() +
                            ".", e);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read maven model from '" + file + "'. Cause: " + e.getMessage() + ".", e);
        } finally {
            IOUtils.closeQuietly(jis);
        }
    }

    /**
     * Returns a JarEntry object if a valid pom file is found in the given jar input stream
     *
     * @param jis Input stream of given jar
     * @return JarEntry object if a pom file is found. Null if not
     * @throws IOException Any exceptions that might occur while using the given stream
     */
    private static JarEntry getPomFile(JarInputStream jis) throws IOException {
        if (jis != null) {
            JarEntry entry;
            while (((entry = jis.getNextJarEntry()) != null)) {
                String name = entry.getName();
                //Look for pom.xml in META-INF/maven/
                if (name.startsWith("META-INF/maven/") && name.endsWith("pom.xml")) {
                    int size = (int) entry.getSize();
                    //Sanity check
                    if (size <= 0) {
                        log.warn("Found pom.xml file with size {} inside the zip. Ignoring", size);
                        entry = null;
                    }
                    return entry;
                }
            }
        }

        return null;
    }

    /**
     * Gathers maven artifact information which was (or was not) managed to gather from the given pom file
     *
     * @param file   Jar file to gather info from
     * @param result MavenArtifactInfo object to append info to
     */
    private static void gatherInfoFromPomFile(File file, MavenArtifactInfo result) {
        try {
            FileInputStream in = new FileInputStream(file);
            try {
                fillArtifactInfoFromPomModel(in, result);
            } finally {
                IOUtils.closeQuietly(in);
            }
            result.setType(MavenArtifactInfo.POM);
        } catch (Exception e) {
            log.debug("Failed to read maven model from '{}'. Cause: {}.", file.getName(), e.getMessage());
            result.setType(MavenArtifactInfo.XML);
        }
    }

    private static void gatherInfoFromIvyFile(File file, MavenArtifactInfo result) {
        result.setType(MavenArtifactInfo.XML);
        try {
            IvyService ivyService = ContextHelper.get().beanForType(IvyService.class);
            ModuleDescriptor ivyDescriptor = ivyService.parseIvyFile(file);
            if (ivyDescriptor != null) {
                ModuleRevisionId ivyModule = ivyDescriptor.getModuleRevisionId();
                result.setGroupId(ivyModule.getOrganisation());
                result.setArtifactId(ivyModule.getName());
                result.setVersion(ivyModule.getRevision());
                result.setClassifier("ivy");
            } else {
                log.debug("Failed to read ivy model from '{}'", file.getName());
            }
        } catch (Exception e) {
            log.debug("Failed to read ivy model from '{}'. Cause: {}.", file.getName(), e.getMessage());
        }
    }

    /**
     * Gathers maven artifact information from the given file in using maven naming matching
     *
     * @param file   Uploaded file to gather info from
     * @param result MavenArtifactInfo object to append info to
     */
    private static void gatherInfoFromMatching(File file, MavenArtifactInfo result) {
        //Try to match file name
        MavenArtifactInfo matchingInfo = MavenNaming.getInfoByMatching(file.getName());

        //Append any info if needed and existing
        if (!result.hasArtifactId() && matchingInfo.hasArtifactId()) {
            result.setArtifactId(matchingInfo.getArtifactId());
        }
        if (!result.hasVersion() && matchingInfo.hasVersion()) {
            result.setVersion(matchingInfo.getVersion());
        }
        if (!result.isBuiltFromPomInfo()) {
            if (!result.hasClassifier() && matchingInfo.hasClassifier()) {
                result.setClassifier(matchingInfo.getClassifier());
            }
        }
    }

    /**
     * Provides a fallback in order to fill in essential but missing information by using the given file's name
     *
     * @param file   Uploaded file to gather info from
     * @param result MavenArtifactInfo object to append info to
     */
    private static void gatheringFallback(File file, MavenArtifactInfo result) {
        String fileName = file.getName();
        String baseFileName = FilenameUtils.getBaseName(fileName);

        //Complete values by falling back to dumb defaults
        if (MavenArtifactInfo.NA.equals(result.getArtifactId())) {
            result.setArtifactId(baseFileName);
        }
        if (MavenArtifactInfo.NA.equals(result.getGroupId())) {
            //If we have no group, set it to be the same as the artifact name
            result.setGroupId(result.getArtifactId());
        }
        if (MavenArtifactInfo.NA.equals(result.getVersion())) {
            result.setVersion(baseFileName);
        }
    }

    private static String readString(Reader reader) throws IOException {
        final StringBuffer buffer = new StringBuffer(2048);
        int value;
        while ((value = reader.read()) != -1) {
            buffer.append((char) value);
        }
        return buffer.toString();
    }
}
