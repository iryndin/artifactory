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

package org.artifactory.repo.index.locator;

import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFsItem;
import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.artifact.GavCalculator;
import org.sonatype.nexus.index.locator.GavHelpedLocator;

import java.io.File;
import java.util.List;

/**
 * @author yoavl
 */
public class PomLocator implements GavHelpedLocator {

    public File locate(File source, GavCalculator gavCalculator, Gav gav) {
        //Get the pom name
        String artifactName = gav.getArtifactId() + "-" + gav.getVersion() + ".pom";
        JcrFile file = (JcrFile) source;
        List<JcrFsItem> children = file.getParentFolder().getItems();
        for (JcrFsItem child : children) {
            if (artifactName.equals(child.getName())) {
                return child;
            }
        }
        return null;
    }
}