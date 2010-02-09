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

import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrZipFile;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.creator.MavenPluginArtifactInfoIndexCreator;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.ZipEntry;

/**
 * @author yoavl
 */
public class JcrMavenPluginArtifactInfoIndexCreator extends MavenPluginArtifactInfoIndexCreator {

    private static final String MAVEN_PLUGIN_PACKAGING = "maven-plugin";

    @Override
    public void populateArtifactInfo(ArtifactContext ac) {
        JcrFile artifact = (JcrFile) ac.getArtifact();

        ArtifactInfo ai = ac.getArtifactInfo();

        // we need the file to perform these checks, and those may be only JARs
        if (artifact != null && MAVEN_PLUGIN_PACKAGING.equals(ai.packaging) && artifact.getName().endsWith(".jar")) {
            // TODO: recheck, is the following true? "Maven plugins and Maven Archetypes can be only JARs?"

            // 1st, check for maven plugin
            checkMavenPlugin(ai, artifact);
        }
    }

    private void checkMavenPlugin(ArtifactInfo ai, JcrFile artifact) {
        JcrZipFile jf = null;

        InputStream is = null;

        try {
            jf = new JcrZipFile(artifact);

            ZipEntry entry = jf.getEntry("META-INF/maven/plugin.xml");

            if (entry == null) {
                return;
            }

            is = new BufferedInputStream(jf.getInputStream(entry));

            PlexusConfiguration plexusConfig =
                    new XmlPlexusConfiguration(Xpp3DomBuilder.build(new InputStreamReader(is)));

            ai.prefix = plexusConfig.getChild("goalPrefix").getValue();

            ai.goals = new ArrayList<String>();

            PlexusConfiguration[] mojoConfigs = plexusConfig.getChild("mojos").getChildren("mojo");

            for (PlexusConfiguration mojoConfig : mojoConfigs) {
                ai.goals.add(mojoConfig.getChild("goal").getValue());
            }
        }
        catch (Exception e) {
            getLogger().info("Failed to parsing Maven plugin " + artifact.getAbsolutePath(), e);
        }
        finally {
            if (jf != null) {
                try {
                    jf.close();
                }
                catch (Exception e) {
                    getLogger().error("Could not close jar file properly.", e);
                }
            }

            IOUtil.close(is);
        }
    }
}