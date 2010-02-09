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

import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrZipFile;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.context.ArtifactIndexingContext;
import org.sonatype.nexus.index.creator.JarFileContentsIndexCreator;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;

/**
 * @author yoavl
 */
public class JcrJarFileContentsIndexCreator extends JarFileContentsIndexCreator {

    @Override
    public void populateArtifactInfo(ArtifactIndexingContext context) throws IOException {
        ArtifactContext artifactContext = context.getArtifactContext();

        ArtifactInfo ai = artifactContext.getArtifactInfo();

        JcrFile artifactFile = (JcrFile) artifactContext.getArtifact();

        if (artifactFile != null && artifactFile.exists() && artifactFile.getName().endsWith(".jar")) {
            updateArtifactInfo(ai, artifactFile);
        }
    }

    private void updateArtifactInfo(ArtifactInfo ai, JcrFile file) {
        int totalClasses = 0;

        JcrZipFile jar = null;

        try {
            jar = new JcrZipFile(file);

            StringBuilder sb = new StringBuilder();

            @SuppressWarnings("unchecked")
            List<? extends ZipEntry> entries = jar.entries();
            for (ZipEntry e : entries) {
                String name = e.getName();

                if (name.endsWith(".class")) {
                    totalClasses++;

                    // TODO verify if class is public or protected
                    // TODO skip all inner classes for now

                    int i = name.lastIndexOf("$");

                    if (i == -1) {
                        sb.append(name.substring(0, name.length() - 6)).append("\n");
                    }
                } else if ("META-INF/archetype.xml".equals(name)
                        || "META-INF/maven/archetype.xml".equals(name)
                        || "META-INF/maven/archetype-metadata.xml".equals(name)) {
                    ai.packaging = "maven-archetype";
                }
            }

            ai.classNames = sb.toString();
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
