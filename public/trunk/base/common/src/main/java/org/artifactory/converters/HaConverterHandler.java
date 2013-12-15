/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.converters;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.version.CompoundVersionDetails;

import java.io.File;

/**
 * Author: gidis
 */
public class HaConverterHandler implements ArtifactoryConverterAdapter {
    private ArtifactoryConverterAdapter[] converters;
    private VersionProvider vm;
    private ArtifactoryHome artifactoryHome;

    public HaConverterHandler(VersionProvider vm, ArtifactoryHome artifactoryHome) {
        this.vm = vm;
        this.artifactoryHome = artifactoryHome;
        File etc = artifactoryHome.getHaEtcFile();
        converters = new ArtifactoryConverterAdapter[]{new LoggingConverter(etc, etc), new MimeTypeConverter(
                artifactoryHome)};
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        for (ArtifactoryConverterAdapter homeConverter : converters) {
            if (homeConverter.isInterested(vm.getOriginalHomeVersionDetails(), vm.getRunningVersionDetails())) {
                homeConverter.convert(source, target);
            }
        }
    }

    @Override
    public void conversionEnded() {
        // Update DB properties
        for (ArtifactoryConverterAdapter converter : converters) {
            converter.conversionEnded();
        }
        // Reload the properties here (otherwise we will reload it twice once for the home and second for the cluster converters)
        artifactoryHome.writeBundledHomeArtifactoryProperties();
    }

    @Override
    public boolean isInterested(CompoundVersionDetails source, CompoundVersionDetails target) {
        if (artifactoryHome.isHaConfigured()) {
            for (ArtifactoryConverterAdapter convert : converters) {
                if (convert.isInterested(source, target)) {
                    return true;
                }
            }
        }
        return false;
    }
}