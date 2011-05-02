/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.update.md.v130beta3;

import org.artifactory.api.md.MetadataEntry;
import org.artifactory.update.md.MetadataConverter;
import org.artifactory.update.md.MetadataConverterUtils;
import org.artifactory.update.md.MetadataType;
import org.artifactory.update.md.MetadataVersion;
import org.artifactory.update.md.current.MetadataReaderImpl;

import java.util.List;

/**
 * @author freds
 * @date Nov 13, 2008
 */
public class MetadataReader130beta3 extends MetadataReaderImpl {

    @Override
    protected MetadataEntry createMetadataEntry(String metadataName, String xmlContent) {
        List<MetadataConverter> converters = null;
        if (ArtifactoryFileConverter.ARTIFACTORY_FILE.equals(metadataName)) {
            converters = MetadataVersion.getConvertersFor(MetadataVersion.v2, MetadataType.file);
        } else if (ArtifactoryFolderConverter.ARTIFACTORY_FOLDER.equals(metadataName)) {
            converters = MetadataVersion.getConvertersFor(MetadataVersion.v2, MetadataType.folder);
        }

        if (converters != null) {
            for (MetadataConverter converter : converters) {
                xmlContent = MetadataConverterUtils.convertString(converter, xmlContent);
                metadataName = converter.getNewMetadataName();
            }
        }
        MetadataEntry metadataEntry = new MetadataEntry(metadataName, xmlContent);
        return metadataEntry;
    }
}
