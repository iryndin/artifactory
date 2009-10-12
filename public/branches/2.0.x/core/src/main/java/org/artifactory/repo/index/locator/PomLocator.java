package org.artifactory.repo.index.locator;

import org.artifactory.repo.LocalRepo;
import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.artifact.M2GavCalculator;

import java.io.File;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class PomLocator extends ChildBasedLocator {
    public PomLocator(LocalRepo localRepo) {
        super(localRepo);
    }

    @Override
    protected String getChildName(File source, Gav gav) {
        return M2GavCalculator.calculateArtifactName(gav);
    }
}