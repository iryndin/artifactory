package org.artifactory.update.md.v125rc0;

import org.artifactory.update.md.MetadataConverter;
import org.artifactory.update.md.MetadataType;
import org.jdom.Document;
import org.jdom.Element;

/**
 * Extracts the stats info (download count) from an old file metadata - before v1.3.0-beta-3.
 *
 * @author Yossi Shaul
 */
public class MdStatsConverter implements MetadataConverter {
    private final String STATS_NAME = "artifactory.stats";

    public String getNewMetadataName() {
        return STATS_NAME;
    }

    public MetadataType getSupportedMetadataType() {
        return MetadataType.stats;
    }

    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Element downloadCount = root.getChild("downloadCount");
        if (downloadCount == null) {
            downloadCount = new Element("downloadCount");
            downloadCount.setText("0");
        }
        // rename the root to the stats name
        root.setName(STATS_NAME);
        // remove all childer
        root.removeContent();
        // add the download count
        root.addContent(downloadCount);
    }
}
