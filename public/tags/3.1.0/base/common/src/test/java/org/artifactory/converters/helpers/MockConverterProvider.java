package org.artifactory.converters.helpers;

import org.artifactory.common.property.ArtifactoryConverter;
import org.artifactory.converters.ConverterProvider;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.CompoundVersionDetails;

import java.util.Date;

/**
 * Author: gidis
 */
public class MockConverterProvider implements ConverterProvider {
    @Override
    public void homeReady() {
    }

    @Override
    public void dbAccessReady() {
    }

    @Override
    public void serviceConvert(ArtifactoryConverter artifactoryConverter) {
    }

    @Override
    public void conversionEnded() {
    }

    @Override
    public boolean isConverting() {
        return false;
    }

    @Override
    public CompoundVersionDetails getRunningVersionDetails() {
        return new CompoundVersionDetails(ArtifactoryVersion.v310, "3.1.0", "1.1", new Date().getTime());
    }

    @Override
    public CompoundVersionDetails getOriginalHaVersionDetails() {
        return new CompoundVersionDetails(ArtifactoryVersion.v310, "3.1.0", "1.1", new Date().getTime());
    }

    @Override
    public CompoundVersionDetails getOriginalHomeVersionDetails() {
        return new CompoundVersionDetails(ArtifactoryVersion.v310, "3.1.0", "1.1", new Date().getTime());
    }

    @Override
    public CompoundVersionDetails getOriginalServiceVersionDetails() {
        return new CompoundVersionDetails(ArtifactoryVersion.v310, "3.1.0", "1.1", new Date().getTime());
    }
}
