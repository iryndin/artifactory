/**
 * Copyright (c) 2007-2008 Sonatype, Inc. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License Version 1.0, which accompanies this distribution and is
 * available at http://www.eclipse.org/legal/epl-v10.html.
 */
/*
 * Additional contributors:
 *    JFrog Ltd.
 */

package org.artifactory.repo.index.creator;

import org.apache.maven.model.Model;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrZipFile;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.index.locator.ExtensionBasedLocator;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.schedule.TaskService;
import org.artifactory.spring.InternalContextHelper;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.index.ArtifactAvailablility;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.creator.MinimalArtifactInfoIndexCreator;
import org.sonatype.nexus.index.locator.Locator;

import java.io.File;
import java.io.IOException;

public class JcrMinimalArtifactInfoIndexCreator extends MinimalArtifactInfoIndexCreator {
    private static final Logger log = LoggerFactory.getLogger(JcrMinimalArtifactInfoIndexCreator.class);

    private final Locator jl;
    private final Locator sl;
    private final Locator sigl;
    //private Locator sha1l = new JcrSha1Locator();

    public JcrMinimalArtifactInfoIndexCreator(StoringRepo repo) {
        this.jl = new ExtensionBasedLocator(repo, "-javadoc.jar");
        this.sl = new ExtensionBasedLocator(repo, "-sources.jar");
        this.sigl = new ExtensionBasedLocator(repo, ".jar.asc");
    }

    @SuppressWarnings({"OverlyComplexMethod"})
    @Override
    public void populateArtifactInfo(ArtifactContext ac) {
        //Test whether the indexer needs to be stopped or paused
        //Check if we need to stop/suspend
        TaskService taskService = InternalContextHelper.get().getTaskService();
        boolean stop = taskService.pauseOrBreak();
        if (stop) {
            return;
        }
        //Populating the artifact info
        JcrFile artifact = (JcrFile) ac.getArtifact();
        JcrFile pom = (JcrFile) ac.getPom();
        ArtifactInfo ai = ac.getArtifactInfo();

        if (pom != null) {
            ai.lastModified = pom.lastModified();

            ai.fextension = "pom";
        }

        // TODO handle artifacts without poms
        if (pom != null) {
            if (ai.classifier != null) {
                ai.sourcesExists = ArtifactAvailablility.NOT_AVAILABLE;

                ai.javadocExists = ArtifactAvailablility.NOT_AVAILABLE;
            } else {
                File sources = sl.locate(pom);
                if (!sources.exists()) {
                    ai.sourcesExists = ArtifactAvailablility.NOT_PRESENT;
                } else {
                    ai.sourcesExists = ArtifactAvailablility.PRESENT;
                }

                File javadoc = jl.locate(pom);
                if (!javadoc.exists()) {
                    ai.javadocExists = ArtifactAvailablility.NOT_PRESENT;
                } else {
                    ai.javadocExists = ArtifactAvailablility.PRESENT;
                }
            }
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
                ac.addError(e);
                if (log.isDebugEnabled()) {
                    log.debug("Could not retrieve artifact checksum.", e);
                } else {
                    String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    log.warn("Could not retrieve artifact checksum" + (msg != null ? ": " + msg : "") + ".");
                }
            }

            ai.lastModified = artifact.lastModified();

            ai.size = artifact.length();

            ai.fextension = getExtension(artifact, ac.getGav());

            if (ai.packaging == null) {
                ai.packaging = ai.fextension;
            }
        }

        Model model = ac.getPomModel();

        if (model != null) {
            ai.name = model.getName();

            ai.description = model.getDescription();

            if (model.getPackaging() != null && ai.classifier == null) {
                // only when this is not a classified artifact
                ai.packaging = model.getPackaging();
            }
        }
    }

    private String getExtension(File artifact, Gav gav) {
        if (gav != null && StringUtils.isNotBlank(gav.getExtension())) {
            return gav.getExtension();
        }

        // last resort, the extension of the file
        String artifactFileName = artifact.getName().toLowerCase();

        // tar.gz? and other "special" combinations
        if (artifactFileName.endsWith("tar.gz")) {
            return "tar.gz";
        } else if (artifactFileName.equals("tar.bz2")) {
            return "tar.bz2";
        }

        // get the part after the last dot
        return FileUtils.getExtension(artifactFileName);
    }

    /*private void checkMavenPlugin(ArtifactInfo ai, JcrFile artifact) {
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
    }*/

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
}