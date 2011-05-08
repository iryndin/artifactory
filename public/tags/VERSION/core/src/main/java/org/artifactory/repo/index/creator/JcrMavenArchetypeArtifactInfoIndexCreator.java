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

import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.creator.MavenArchetypeArtifactInfoIndexCreator;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrZipFile;

import java.util.zip.ZipEntry;

/**
 * @author yoavl
 */
public class JcrMavenArchetypeArtifactInfoIndexCreator extends MavenArchetypeArtifactInfoIndexCreator {

    private static final String MAVEN_ARCHETYPE_PACKAGING = "maven-archetype";

    private static final String[] ARCHETYPE_XML_LOCATIONS =
            {"META-INF/maven/archetype.xml", "META-INF/archetype.xml", "META-INF/maven/archetype-metadata.xml"};

    @Override
    public void populateArtifactInfo(ArtifactContext ac) {
        JcrFile artifact = (JcrFile) ac.getArtifact();

        ArtifactInfo ai = ac.getArtifactInfo();

        // we need the file to perform these checks, and those may be only JARs
        if (artifact != null && !MAVEN_ARCHETYPE_PACKAGING.equals(ai.packaging)
                && artifact.getName().endsWith(".jar")) {
            // TODO: recheck, is the following true? "Maven plugins and Maven Archetypes can be only JARs?"

            // check for maven archetype, since Archetypes seems to not have consistent packaging,
            // and depending on the contents of the JAR, this call will override the packaging to "maven-archetype"!
            checkMavenArchetype(ai, artifact);
        }
    }

    /**
     * Archetypes that are added will have their packaging types set correctly (to maven-archetype)
     *
     * @param ai
     * @param artifact
     */
    private void checkMavenArchetype(ArtifactInfo ai, JcrFile artifact) {
        JcrZipFile jf = null;

        try {
            jf = new JcrZipFile(artifact);

            for (String location : ARCHETYPE_XML_LOCATIONS) {
                if (checkEntry(ai, jf, location)) {
                    return;
                }
            }
        }
        catch (Exception e) {
            getLogger().info("Failed to parse Maven artifact " + artifact.getAbsolutePath(), e);
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
        }
    }

    private boolean checkEntry(ArtifactInfo ai, JcrZipFile jf, String entryName) {
        ZipEntry entry = jf.getEntry(entryName);

        if (entry != null) {
            ai.packaging = MAVEN_ARCHETYPE_PACKAGING;

            return true;
        }
        return false;
    }
}