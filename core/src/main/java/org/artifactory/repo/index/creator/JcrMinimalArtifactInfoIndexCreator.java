package org.artifactory.repo.index.creator;

import org.apache.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.artifactory.io.checksum.ChecksumType;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrZipFile;
import org.artifactory.repo.index.locator.JcrJavadocLocator;
import org.artifactory.repo.index.locator.JcrSignatureLocator;
import org.artifactory.repo.index.locator.JcrSourcesLocator;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.artifact.M2GavCalculator;
import org.sonatype.nexus.index.ArtifactAvailablility;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.context.ArtifactIndexingContext;
import org.sonatype.nexus.index.creator.MinimalArtifactInfoIndexCreator;
import org.sonatype.nexus.index.locator.Locator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.ZipEntry;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrMinimalArtifactInfoIndexCreator extends MinimalArtifactInfoIndexCreator {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrMinimalArtifactInfoIndexCreator.class);

    private ModelReader modelReader = new JcrModelReader();
    private Locator jl = new JcrJavadocLocator();
    private Locator sl = new JcrSourcesLocator();
    private Locator sigl = new JcrSignatureLocator();
    //private Locator sha1l = new JcrSha1Locator();

    @SuppressWarnings({"OverlyComplexMethod", "EmptyCatchBlock"})
    @Override
    public void populateArtifactInfo(ArtifactIndexingContext context) {
        ArtifactContext artifactContext = context.getArtifactContext();
        JcrFile artifact = (JcrFile) artifactContext.getArtifact();
        JcrFile pom = (JcrFile) artifactContext.getPom();
        ArtifactInfo ai = artifactContext.getArtifactInfo();
        if (pom != null) {
            Model model = modelReader.readModel(pom, ai.groupId, ai.artifactId, ai.version);
            if (model != null) {
                ai.name = model.getName();
                ai.description = model.getDescription();
                ai.packaging = model.getPackaging() == null ? "jar" : model.getPackaging();

                // look for archetypes
                if (!"maven-archetype".equals(ai.packaging) && //
                        artifact != null && //
                        ("maven-plugin".equals(ai.packaging)//
                                || ai.artifactId.indexOf("archetype") > -1//
                                || ai.groupId.indexOf("archetype") > -1)) {
                    JcrZipFile jf = null;
                    try {
                        jf = new JcrZipFile(artifact);

                        if (jf.getEntry("META-INF/archetype.xml") != null//
                                || jf.getEntry("META-INF/maven/archetype.xml") != null
                                || jf.getEntry("META-INF/maven/archetype-metadata.xml") != null) {
                            ai.packaging = "maven-archetype";
                        }
                    } catch (Exception e) {
                    }
                    finally {
                        close(jf);
                    }
                }
            }

            if ("maven-plugin".equals(ai.packaging) && artifact != null) {
                JcrZipFile jf = null;

                InputStream is = null;

                try {
                    jf = new JcrZipFile(artifact);

                    ZipEntry entry = jf.getEntry("META-INF/maven/plugin.xml");

                    if (entry != null) {
                        is = jf.getInputStream(entry);

                        PluginDescriptorBuilder builder = new PluginDescriptorBuilder();

                        PluginDescriptor descriptor = builder.build(new InputStreamReader(is));

                        ai.prefix = descriptor.getGoalPrefix();

                        ai.goals = new ArrayList<String>();

                        for (Object o : descriptor.getMojos()) {
                            ai.goals.add(((MojoDescriptor) o).getGoal());
                        }
                    }
                }
                catch (Exception e) {
                }
                finally {
                    close(jf);
                    IOUtil.close(is);
                }
            }

            Gav gav = M2GavCalculator.calculate(//
                    ai.groupId.replace('.', '/') + '/'//
                            + ai.artifactId + '/'//
                            + ai.version + '/'//
                            + (artifact == null ? ai.artifactId + '-' + ai.version + ".jar" :
                            artifact.getName()));


            try {
                String sha1 = artifact.getChecksum(ChecksumType.sha1);
                ai.sha1 = StringUtils.chomp(sha1).trim().split(" ")[0];
            } catch (Exception e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Could not retrieve artifact checksum.", e);
                } else {
                    String msg =
                            e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    LOGGER.warn("Could not retrieve artifact checksum: " + msg);
                }
            }

            File sources = sl.locate(pom, gav);

            ai.sourcesExists = sources.exists() ? ArtifactAvailablility.PRESENT
                    : ArtifactAvailablility.NOT_PRESENT;

            File javadoc = jl.locate(pom, gav);

            ai.javadocExists = javadoc.exists() ? ArtifactAvailablility.PRESENT
                    : ArtifactAvailablility.NOT_PRESENT;

            File signature = sigl.locate(pom, gav);

            ai.signatureExists = signature.exists() ? ArtifactAvailablility.PRESENT
                    : ArtifactAvailablility.NOT_PRESENT;
        }

        if (artifact != null) {
            ai.lastModified = artifact.lastModified();

            ai.size = artifact.length();
        }
    }

    @SuppressWarnings({"EmptyCatchBlock"})
    private static void close(JcrZipFile zf) {
        if (zf != null) {
            try {
                zf.close();
            }
            catch (IOException ex) {
            }
        }
    }
}