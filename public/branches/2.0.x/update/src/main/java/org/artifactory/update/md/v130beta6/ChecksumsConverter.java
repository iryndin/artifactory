package org.artifactory.update.md.v130beta6;

import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.update.md.MetadataConverter;
import org.artifactory.update.md.MetadataType;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts artifactory-file.xml from version 1.3.0-beta-6 to 1.3.0-rc1. The extension node needs to be renamed and the
 * checksums are organized differently.
 *
 * @author Yossi Shaul
 */
public class ChecksumsConverter implements MetadataConverter {
    private final static Logger log = LoggerFactory.getLogger(ChecksumsConverter.class);
    public static final String ARTIFACTORY_FILE = "artifactory-file";

    public void convert(Document doc) {
        Element root = doc.getRootElement();

        Element extension = root.getChild("extension");

        // rename the extensions to additionalInfo
        extension.setName("additionalInfo");

        // now get the sha1 and md5 values

        Element sha1Node = extension.getChild("sha1");
        String sha1 = null;
        if (sha1Node != null) {
            sha1 = sha1Node.getValue();
            extension.removeContent(sha1Node);
        } else {
            log.debug("SHA1 checksum not found, setting it to {}", sha1);
        }

        Element md5Node = extension.getChild("md5");
        String md5 = null;
        if (md5Node != null) {
            md5 = md5Node.getValue();
            extension.removeContent(md5Node);
        } else {
            log.debug("MD5 checksum not found, setting it to {}", md5);
        }

        // create the new checksums nodes
        Element checksumsInfo = new Element("checksumsInfo");
        extension.addContent(checksumsInfo);
        Element checksums = new Element("checksums");
        checksumsInfo.addContent(checksums);
        addChecksum(checksums, ChecksumType.sha1, sha1);
        addChecksum(checksums, ChecksumType.md5, md5);
    }

    private void addChecksum(Element checksums, ChecksumType type, String checksum) {
        Element checksumElement = new Element("checksum");
        checksumElement.addContent(createTextElement("type", type.name()));
        checksumElement.addContent(createTextElement("original", ChecksumInfo.TRUSTED_FILE_MARKER));
        checksumElement.addContent(createTextElement("actual", checksum));
        checksums.addContent(checksumElement);
    }

    private Element createTextElement(String elementName, String value) {
        Element element = new Element(elementName);
        element.setText(value);
        return element;
    }

    public String getNewMetadataName() {
        return ARTIFACTORY_FILE;
    }

    public MetadataType getSupportedMetadataType() {
        return MetadataType.file;
    }

}
