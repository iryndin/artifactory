package org.artifactory.update.md.v130beta6;

import org.artifactory.api.md.MetadataEntry;
import org.artifactory.update.md.MetadataConverter;
import org.artifactory.update.md.MetadataConverterUtils;
import org.artifactory.update.md.MetadataType;
import org.artifactory.update.md.MetadataVersion;
import org.artifactory.update.md.current.MetadataReaderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Reads and converts metadata from version 1.3.0-beta-6.
 *
 * @author Yossi Shaul
 */
public class MetadataReader130beta6 extends MetadataReaderImpl {
    private final static Logger log = LoggerFactory.getLogger(MetadataReader130beta6.class);

    @Override
    protected MetadataEntry createMetadataEntry(String metadataName, String xmlContent) {
        List<MetadataConverter> converters = null;
        if ("artifactory-file".equals(metadataName)) {
            converters = MetadataVersion.getConvertersFor(MetadataVersion.v130beta6, MetadataType.file);
        } else if ("artifactory-folder".equals(metadataName)) {
            converters = MetadataVersion.getConvertersFor(MetadataVersion.v130beta6, MetadataType.folder);
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
