package org.artifactory.repo.index.locator;

import org.artifactory.jcr.fs.JcrFile;
import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.index.locator.Locator;

import java.io.File;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrSignatureLocator implements Locator {
    public File locate(File source, Gav gav) {
        return new JcrFile(source.getAbsolutePath().replaceAll(".pom", ".jar.asc"));
    }
}