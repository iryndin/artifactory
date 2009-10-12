package org.artifactory.version;

import org.artifactory.api.version.ArtifactoryVersioning;
import org.artifactory.api.version.VersionHolder;
import org.jdom.Document;
import org.jdom.Element;

import java.util.List;

/**
 * A tool to parse versioning info XML into an ArtifactoryVersioning
 *
 * @author Noam Tenne
 */
public class VersionParser {

    /**
     * Parses the recieved input string info an ArtifactoryVersioning object
     *
     * @param input String containing the XML of the versioning info file
     * @return ArtifactoryVersioning - Object representation of the versioning info xml
     */
    public static ArtifactoryVersioning parse(String input) {
        Document doc = ConverterUtils.parse(input);
        Element root = doc.getRootElement();
        Element version = root.getChild("versioning");
        if (version == null) {
            throw new RuntimeException("No version is defined");
        }

        VersionHolder latest = null;
        VersionHolder release = null;
        List children = version.getChildren();
        for (Object child : children) {
            Element holder = (Element) child;
            String versionNumber = holder.getChildText("version");
            String revisionNumber = holder.getChildText("revision");
            String wikiUrl = holder.getChildText("wikiUrl");
            String downloadUrl = holder.getChildText("downloadUrl");
            if ("latest".equals(holder.getName())) {
                latest = new VersionHolder(versionNumber, revisionNumber, wikiUrl, downloadUrl);
            } else if ("release".equals(holder.getName())) {
                release = new VersionHolder(versionNumber, revisionNumber, wikiUrl, downloadUrl);
            }
        }

        if ((latest == null) || (release == null)) {
            throw new RuntimeException(
                    "Latest and stable version and revisions are not defined properly");
        }

        return new ArtifactoryVersioning(latest, release);
    }
}
