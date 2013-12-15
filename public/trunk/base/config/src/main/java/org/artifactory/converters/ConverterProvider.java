package org.artifactory.converters;


import org.artifactory.common.property.ArtifactoryConverter;

public interface ConverterProvider extends VersionProvider {

    void homeReady();

    void dbAccessReady();

    void serviceConvert(ArtifactoryConverter artifactoryConverter);

    void conversionEnded();

    boolean isConverting();
}