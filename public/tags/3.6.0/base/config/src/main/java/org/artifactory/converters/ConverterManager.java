package org.artifactory.converters;


import org.artifactory.common.property.ArtifactoryConverter;

public interface ConverterManager {

    void serviceConvert(ArtifactoryConverter artifactoryConverter);

    void afterAllInits();

    void afterContextReady();

    boolean isConverting();
}