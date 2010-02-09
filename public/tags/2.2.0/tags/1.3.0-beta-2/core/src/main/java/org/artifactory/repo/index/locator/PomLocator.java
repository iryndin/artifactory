package org.artifactory.repo.index.locator;

import org.artifactory.jcr.fs.JcrFile;
import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.artifact.M2GavCalculator;
import org.sonatype.nexus.index.locator.Locator;

import java.io.File;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class PomLocator implements Locator {
    public File locate(File source, Gav gav) {
        return new JcrFile(source.getParent(), M2GavCalculator.calculateArtifactName(gav));
    }
}