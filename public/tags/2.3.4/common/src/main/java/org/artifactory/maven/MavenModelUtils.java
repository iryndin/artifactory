/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.ivy.IvyNaming;
import org.artifactory.ivy.IvyService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.PathUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @author Yoav Landman
 */
public abstract class MavenModelUtils {
    private static final Logger log = LoggerFactory.getLogger(MavenModelUtils.class);
    public static final String UTF8 = "utf-8";
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final String UTC_TIMESTAMP_PATTERN = "yyyyMMdd.HHmmss";

    private MavenModelUtils() {
        // utility class
    }

    /**
     * @param date Date to convert
     * @return Maven unique snapshot version timestamp for the input date
     */
    public static String dateToUniqueSnapshotTimestamp(Date date) {
        return getUtcDateFormatter().format(date);
    }

    public static Date uniqueSnapshotToUtc(String timestamp) {
        try {
            return getUtcDateFormatter().parse(timestamp);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to transfer timestamp to date.", e);
        }
    }

    private static DateFormat getUtcDateFormatter() {
        DateFormat utcDateFormatter = new SimpleDateFormat(UTC_TIMESTAMP_PATTERN);
        utcDateFormatter.setTimeZone(UTC_TIME_ZONE);
        return utcDateFormatter;
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
        return toMavenMetadata(new InputStreamReader(metadataStream, UTF8));
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
            return metadataReader.read(reader, false);
        } catch (XmlPullParserException e) {
            throw new IOException("Failed to parse metadata: " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    /**
     * Build custom maven-metadata.xml according to a specific version.
     *
     * @param artifactInfo The original {@code MavenArtifactInfo} to assemble the maven metadata according to the same
     *                     gid,aid,and version, {@link Versioning#setLastUpdatedTimestamp(Date)} is updated to now. and
     *                     the build number and timestamp in the {@link Snapshot} is set according to the name.
     * @param fileName     The file name
     * @return The custom maven-metadata.xml
     */
    public static Metadata buildSnapshotMavenMetadata(MavenArtifactInfo artifactInfo, String fileName) {
        Metadata metadata = new Metadata();
        metadata.setGroupId(artifactInfo.getGroupId());
        metadata.setArtifactId(artifactInfo.getArtifactId());
        metadata.setVersion(artifactInfo.getVersion());
        Versioning versioning = new Versioning();
        metadata.setVersioning(versioning);
        versioning.setLastUpdatedTimestamp(new Date());
        Snapshot snapshot = new Snapshot();
        versioning.setSnapshot(snapshot);
        snapshot.setBuildNumber(MavenNaming.getUniqueSnapshotVersionBuildNumber(fileName));
        snapshot.setTimestamp(MavenNaming.getUniqueSnapshotVersionTimestamp(fileName));
        return metadata;
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

    /**
     * @param artifactInfo Maven artifact info to build the model from
     * @return A maven {@link Model} matching the values of the maven artifact info.
     */
    public static Model toMavenModel(MavenArtifactInfo artifactInfo) {
        return new MavenPomBuilder().groupId(artifactInfo.getGroupId()).artifactId(artifactInfo.getArtifactId())
                .version(artifactInfo.getVersion()).packaging(artifactInfo.getType()).build();
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

    /**
     * @param pomInputStream Input stream of the pom content.
     * @return Maven artifact info built from the pom data.
     */
    public static MavenArtifactInfo mavenModelToArtifactInfo(InputStream pomInputStream)
            throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        InputStreamReader pomStream = new InputStreamReader(pomInputStream, UTF8);
        Model model = reader.read(pomStream);
        return mavenModelToArtifactInfo(model);
    }

    public static MavenArtifactInfo mavenModelToArtifactInfo(Model model) {
        Parent parent = model.getParent();
        String groupId = model.getGroupId();
        if (groupId == null && parent != null) {
            groupId = parent.getGroupId();
        }
        MavenArtifactInfo artifactInfo = new MavenArtifactInfo();
        artifactInfo.setGroupId(groupId);
        artifactInfo.setArtifactId(model.getArtifactId());
        String version = model.getVersion();
        if (version == null && parent != null) {
            version = parent.getVersion();
        }
        artifactInfo.setVersion(version);
        return artifactInfo;
    }

    /**
     * Returns a maven artifact info after gathering information from the given file
     *
     * @param file File to gather information from
     * @return MavenArtifactInfo object containing gathered information
     */
    public static MavenArtifactInfo artifactInfoFromFile(File file) {
        MavenArtifactInfo result;
        result = attemptToBuildInfoFromModel(file);
        if (result != null) {
            // built from model - most accurate, we're done
            return result;
        }

        // no info from a model, try to guess as good as possible based on the file name and path
        result = MavenNaming.getInfoByMatching(file.getName());
        fillMissingRequiredFields(file, result);
        return result;
    }

    /**
     * Attempt to gather maven artifact information from the given file based on a model (pom, ivy etc.).
     *
     * @param file Uploaded file to gather info from
     * @return Maven artifact info based on the model, null if model not found or couldn't be parsed
     */
    private static MavenArtifactInfo attemptToBuildInfoFromModel(File file) {
        MavenArtifactInfo result = null;
        String fileName = file.getName();
        if (NamingUtils.isJarVariant(fileName)) {
            //File is a jar variant
            result = gatherInfoFromJarFile(file);
        } else if (MavenNaming.isClientOrServerPom(fileName)) {
            result = gatherInfoFromPomFile(file);
        } else if (IvyNaming.isIvyFileName(fileName)) {
            result = gatherInfoFromIvyFile(file);
        }
        return result;
    }

    /**
     * Gathers maven artifact information which was (or was not) managed to gather from the given Jar file
     *
     * @param file Jar file to gather info from
     */

    private static MavenArtifactInfo gatherInfoFromJarFile(File file) {
        MavenArtifactInfo artifactInfo = null;
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
                    artifactInfo = mavenModelToArtifactInfo(jis);
                    artifactInfo.setType(PathUtils.getExtension(file.getPath()));
                } catch (Exception e) {
                    log.warn("Failed to read maven model from '" + entry.getName() + "'. Cause: " + e.getMessage() +
                            ".", e);
                    artifactInfo = null;
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read maven model from '" + file + "'. Cause: " + e.getMessage() + ".", e);
        } finally {
            IOUtils.closeQuietly(jis);
        }
        return artifactInfo;
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
                    return entry;
                }
            }
        }
        return null;
    }

    /**
     * @param file The file from which to try to extract the POM entry from.
     * @return The POM from the JAR in its String representation.
     */
    public static String getPomFileAsStringFromJar(File file) {
        JarEntry pomEntry;
        JarInputStream inputStream = null;
        try {
            inputStream = new JarInputStream(new FileInputStream(file));
            pomEntry = getPomFile(inputStream);
            if (pomEntry != null) {
                return IOUtils.toString(inputStream);
            }
        } catch (IOException e) {
            log.warn("Unable to read JAR to extract the POM from it.");
            // If the file has a corrupt file, the following error will be thrown.
            // See java.util.zip.ZipInputStream.getUTF8String()
        } catch (IllegalArgumentException iae) {
            log.warn("Unable to read JAR to extract the POM from it.");
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return null;
    }

    /**
     * Gathers maven artifact information which was (or was not) managed to gather from the given pom file
     *
     * @param file Jar file to gather info from
     * @return MavenArtifactInfo object to append info to, null if pom parsing failed
     */
    private static MavenArtifactInfo gatherInfoFromPomFile(File file) {
        MavenArtifactInfo result = null;
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            result = mavenModelToArtifactInfo(in);
            result.setType(MavenArtifactInfo.POM);
        } catch (Exception e) {
            log.debug("Failed to read maven model from '{}'. Cause: {}.", file.getName(), e.getMessage());
        } finally {
            IOUtils.closeQuietly(in);
        }
        return result;
    }

    private static MavenArtifactInfo gatherInfoFromIvyFile(File file) {
        MavenArtifactInfo result = null;
        try {
            IvyService ivyService = ContextHelper.get().beanForType(IvyService.class);
            ModuleDescriptor ivyDescriptor = ivyService.parseIvyFile(file);
            if (ivyDescriptor != null) {
                ModuleRevisionId ivyModule = ivyDescriptor.getModuleRevisionId();
                result = new MavenArtifactInfo();
                result.setGroupId(ivyModule.getOrganisation());
                result.setArtifactId(ivyModule.getName());
                result.setVersion(ivyModule.getRevision());
                result.setClassifier("ivy");
                result.setType(MavenArtifactInfo.XML);
            } else {
                log.debug("Failed to read ivy model from '{}'", file.getName());
            }
        } catch (Exception e) {
            log.debug("Failed to read ivy model from '{}'. Cause: {}.", file.getName(), e.getMessage());
        }
        return result;
    }

    /**
     * Provides a fallback in order to fill in essential but missing information by using the given file's name
     *
     * @param file   Uploaded file to gather info from
     * @param result MavenArtifactInfo object to append info to
     */
    private static void fillMissingRequiredFields(File file, MavenArtifactInfo result) {
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

        // fill the type if the extension is not null and the result holds the default (jar) or is NA
        String extension = PathUtils.getExtension(fileName);
        if (extension != null &&
                (MavenArtifactInfo.NA.equals(result.getType()) || MavenArtifactInfo.JAR.equals(result.getType()))) {
            result.setType(extension);
        }
    }
}
