package org.artifactory.repo.index.locator;

import org.artifactory.jcr.fs.JcrFile;
import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.index.locator.Locator;

import java.io.File;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class MetadataLocator implements Locator {
    public File locate(File source, Gav gav) {
        return new JcrFile(source.getParentFile().getParentFile(), "maven-metadata.xml");
    }
}