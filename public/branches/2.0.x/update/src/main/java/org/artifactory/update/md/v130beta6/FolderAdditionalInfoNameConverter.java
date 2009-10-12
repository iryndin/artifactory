package org.artifactory.update.md.v130beta6;

import org.artifactory.update.md.MetadataConverter;
import org.artifactory.update.md.MetadataType;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts FolderInfo from version 1.3.0-beta-6. Just renaming extension to additionalInfo.
 *
 * @author Yossi Shaul
 */
public class FolderAdditionalInfoNameConverter implements MetadataConverter {
    private final static Logger log = LoggerFactory.getLogger(FolderAdditionalInfoNameConverter.class);
    public static final String ARTIFACTORY_FOLDER = "artifactory-folder";

    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Element extension = root.getChild("extension");
        if (extension != null) {
            extension.setName("additionalInfo");
        } else {
            log.warn("Folder info extension node not found");
        }
    }

    public String getNewMetadataName() {
        // metadata name not changed
        return ARTIFACTORY_FOLDER;
    }

    public MetadataType getSupportedMetadataType() {
        return MetadataType.folder;
    }

}
