/**
 * Copyright (c) 2007-2008 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
/*
 * Additional contributors:
 *    JFrog Ltd.
 */

package org.artifactory.repo.index.locator;

import org.apache.maven.index.artifact.Gav;
import org.apache.maven.index.artifact.GavCalculator;
import org.apache.maven.index.locator.GavHelpedLocator;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.sapi.fs.VfsItem;

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
        List<VfsItem> children = file.getParentFolder().getItems(false);
        for (VfsItem child : children) {
            if (artifactName.equals(child.getName())) {
                return (File) child;
            }
        }
        return null;
    }
}