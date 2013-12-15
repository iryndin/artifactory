package org.artifactory.converters;

import org.artifactory.common.property.ArtifactoryConverter;
import org.artifactory.version.CompoundVersionDetails;

/**
 * Author: gidis
 */
public interface ArtifactoryConverterAdapter extends ArtifactoryConverter {

    boolean isInterested(CompoundVersionDetails source, CompoundVersionDetails target);

    void conversionEnded();
}
