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

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.mime.version.MimeTypesVersion;
import org.artifactory.version.CompoundVersionDetails;

import java.io.File;

/**
 * Author: gidis
 */
public class MimeTypeConverter implements ArtifactoryConverterAdapter {
    private final File mimeTypesFile;

    public MimeTypeConverter(ArtifactoryHome artifactoryHome) {
        mimeTypesFile = artifactoryHome.getMimeTypesFile();
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        if (!mimeTypesFile.exists()) {
            throw new RuntimeException(
                    "Couldn't start Artifactory. Mime types file is missing: " + mimeTypesFile.getAbsolutePath());
        }

        try {
            String mimeTypesXml = Files.toString(mimeTypesFile, Charsets.UTF_8);
            MimeTypesVersion mimeTypesVersion = MimeTypesVersion.findVersion(mimeTypesXml);
            if (!mimeTypesVersion.isCurrent()) {
                String result = mimeTypesVersion.convert(mimeTypesXml);
                Files.write(result, mimeTypesFile, Charsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute mimetypes conversion", e);
        }
    }

    @Override
    public boolean isInterested(CompoundVersionDetails source, CompoundVersionDetails target) {
        if (!mimeTypesFile.exists()) {
            return false;
        }
        try {
            String mimeTypesXml = Files.toString(mimeTypesFile, Charsets.UTF_8);
            MimeTypesVersion mimeTypesVersion = MimeTypesVersion.findVersion(mimeTypesXml);
            return !mimeTypesVersion.isCurrent();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute mimetypes conversion", e);

        }
    }

    @Override
    public void conversionEnded() {
    }
}
