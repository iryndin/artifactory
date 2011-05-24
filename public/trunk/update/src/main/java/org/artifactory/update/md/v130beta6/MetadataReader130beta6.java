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

package org.artifactory.update.md.v130beta6;

import org.artifactory.api.md.MetadataEntry;
import org.artifactory.log.LoggerFactory;
import org.artifactory.update.md.MetadataConverter;
import org.artifactory.update.md.MetadataConverterUtils;
import org.artifactory.update.md.MetadataType;
import org.artifactory.update.md.MetadataVersion;
import org.artifactory.update.md.current.MetadataReaderImpl;
import org.slf4j.Logger;

import java.util.List;

/**
 * Reads and converts metadata from version 1.3.0-beta-6.
 *
 * @author Yossi Shaul
 */
public class MetadataReader130beta6 extends MetadataReaderImpl {
    private static final Logger log = LoggerFactory.getLogger(MetadataReader130beta6.class);

    @Override
    protected MetadataEntry createMetadataEntry(String metadataName, String xmlContent) {
        List<MetadataConverter> converters = null;
        if ("artifactory-file".equals(metadataName)) {
            converters = MetadataVersion.getConvertersFor(MetadataVersion.v3, MetadataType.file);
        } else if ("artifactory-folder".equals(metadataName)) {
            converters = MetadataVersion.getConvertersFor(MetadataVersion.v3, MetadataType.folder);
        } else {
            log.debug("No converter for {} ", metadataName);
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
