/*
 * This file is part of Artifactory.
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

package org.artifactory.repo.index.creator;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrZipFile;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.index.locator.ExtensionBasedLocator;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.schedule.TaskService;
import org.artifactory.spring.InternalContextHelper;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.sonatype.nexus.artifact.Gav;
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
    private static final Logger log = LoggerFactory.getLogger(JcrMinimalArtifactInfoIndexCreator.class);

    private final ModelReader modelReader;
    private final Locator jl;
    private final Locator sl;
    private final Locator sigl;
    //private Locator sha1l = new JcrSha1Locator();

    public JcrMinimalArtifactInfoIndexCreator(StoringRepo repo) {
        this.modelReader = new JcrModelReader();
        this.jl = new ExtensionBasedLocator(repo, "-javadoc.jar");
        this.sl = new ExtensionBasedLocator(repo, "-sources.jar");
        this.sigl = new ExtensionBasedLocator(repo, ".jar.asc");
    }

    @SuppressWarnings({"OverlyComplexMethod", "EmptyCatchBlock"})
    @Override
    public void populateArtifactInfo(ArtifactIndexingContext context) {
        //Test whether the indexer needs to be stopped or paused
        //Check if we need to stop/suspend
        TaskService taskService = InternalContextHelper.get().getTaskService();
        boolean stop = taskService.pauseOrBreak();
        if (stop) {
            return;
        }
        //Populating the artifact info
        ArtifactContext artifactContext = context.getArtifactContext();
        JcrFile artifact = (JcrFile) artifactContext.getArtifact();
        JcrFile pom = (JcrFile) artifactContext.getPom();
        ArtifactInfo ai = artifactContext.getArtifactInfo();

        if (pom != null) {
            Model model = modelReader.readModel(pom, ai.groupId, ai.artifactId, ai.version);
            if (model != null) {
                ai.name = model.getName();
                ai.description = model.getDescription();
                if (model.getPackaging() != null && ai.classifier == null) {
                    // only when this is not a classified artifact
                    ai.packaging = model.getPackaging();
                }
            }
            ai.lastModified = pom.lastModified();
            ai.fextension = "pom";
        }

        if (pom != null && ai.classifier == null) {
            File sources = sl.locate(pom);
            ai.sourcesExists = sources.exists() ? ArtifactAvailablility.PRESENT : ArtifactAvailablility.NOT_PRESENT;

            File javadoc = jl.locate(pom);
            ai.javadocExists = javadoc.exists() ? ArtifactAvailablility.PRESENT : ArtifactAvailablility.NOT_PRESENT;
        } else if (pom != null && ai.classifier != null) {
            ai.sourcesExists = ArtifactAvailablility.NOT_AVAILABLE;

            ai.javadocExists = ArtifactAvailablility.NOT_AVAILABLE;
        }

        if (artifact != null) {
            File signature = sigl.locate(artifact);
            ai.signatureExists = signature.exists() ? ArtifactAvailablility.PRESENT : ArtifactAvailablility.NOT_PRESENT;
            try {
                String sha1 = artifact.getChecksum(ChecksumType.sha1);
                if (sha1 != null) {
                    ai.sha1 = StringUtils.chomp(sha1).trim().split(" ")[0];
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Could not retrieve artifact checksum.", e);
                } else {
                    String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    log.warn("Could not retrieve artifact checksum" + (msg != null ? ": " + msg : "") + ".");
                }
            }

            ai.lastModified = artifact.lastModified();

            ai.size = artifact.length();

            ai.fextension = getExtension(artifact, artifactContext.getGav());

            if (ai.packaging == null) {
                ai.packaging = ai.fextension;
            }
        }
        checkMavenPlugin(ai, artifact);
    }

    private void checkMavenPlugin(ArtifactInfo ai, JcrFile artifact) {
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
                //you bad bad exception swallowers
            }
            finally {
                close(jf);
                IOUtil.close(is);
            }
        }
    }

    private static void close(JcrZipFile zf) {
        if (zf != null) {
            try {
                zf.close();
            }
            catch (IOException ex) {
                // nothing to do
            }
        }
    }

    private String getExtension(File artifact, Gav gav) {
        if (gav != null) {
            return gav.getExtension();
        } else {
            // last resort, the extension of the file
            String artifactFileName = artifact.getName().toLowerCase();

            // tar.gz? and other "special" combinations?
            if (artifactFileName.endsWith("tar.gz")) {
                return "tar.gz";
            } else if ("tar.bz2".equals(artifactFileName)) {
                return "tar.bz2";
            } else {
                // javadoc: gets the part _AFTER_ last dot!
                return FileUtils.getExtension(artifactFileName);
            }
        }
    }
}