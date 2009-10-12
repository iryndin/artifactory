package org.artifactory.repo.index.locator;

import org.artifactory.repo.LocalRepo;
import org.sonatype.nexus.artifact.Gav;

import java.io.File;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class MetadataLocator extends ChildBasedLocator {
    public MetadataLocator(LocalRepo localRepo) {
        super(localRepo);
    }

    protected String getChildName(File source, Gav gav) {
        return "maven-metadata.xml";
    }
}