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
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.creator.JarFileContentsIndexCreator;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;

/**
 * @author yoavl
 */
public class JcrJarFileContentsIndexCreator extends JarFileContentsIndexCreator {

    @Override
    public void populateArtifactInfo(ArtifactContext artifactContext)
            throws IOException {
        ArtifactInfo ai = artifactContext.getArtifactInfo();

        JcrFile artifactFile = (JcrFile) artifactContext.getArtifact();

        if (artifactFile != null && artifactFile.exists() && artifactFile.getName().endsWith(".jar")) {
            updateArtifactInfo(ai, artifactFile);
        }
    }

    private void updateArtifactInfo(ArtifactInfo ai, JcrFile file)
            throws IOException {
        JcrZipFile jar = null;

        try {
            jar = new JcrZipFile(file);

            StringBuilder sb = new StringBuilder();

            @SuppressWarnings("unchecked")
            List<? extends ZipEntry> entries = jar.entries();
            for (ZipEntry e : entries) {
                String name = e.getName();

                if (name.endsWith(".class")) {
                    // TODO verify if class is public or protected
                    // TODO skip all inner classes for now

                    int i = name.indexOf('$');

                    if (i == -1) {
                        if (name.charAt(0) != '/') {
                            sb.append('/');
                        }

                        // class name without ".class"
                        sb.append(name.substring(0, name.length() - 6)).append('\n');
                    }
                }
            }

            if (sb.toString().trim().length() != 0) {
                ai.classNames = sb.toString();
            } else {
                ai.classNames = null;
            }
        }
        finally {
            if (jar != null) {
                try {
                    jar.close();
                }
                catch (Exception e) {
                    getLogger().error("Could not close jar file properly.", e);
                }
            }
        }
    }
}
