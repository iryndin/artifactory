package org.artifactory.version.converter;

import org.artifactory.version.ArtifactoryConfigVersion;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.List;

/**
 * Changes the namespace and the schema location to the latest.
 *
 * @author Yossi Shaul
 */
public class NamespaceConverter implements XmlConverter {
    @SuppressWarnings({"unchecked"})
    public void convert(Document doc) {
        // change the xsd uri and schema location
        String currentXsdUri = ArtifactoryConfigVersion.getCurrent().getXsdUri();
        String currentXsdLocation = ArtifactoryConfigVersion.getCurrent().getXsdLocation();
        Namespace ns = Namespace.getNamespace(currentXsdUri);
        Element root = doc.getRootElement();
        // Check that schema instance namespace is there before adding schema location...
        Namespace schemaInstanceNS = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        List<Namespace> namespaces = root.getAdditionalNamespaces();
        boolean hasSchemaInstanceNS = false;
        for (Namespace namespace : namespaces) {
            // The equality is only on URI so hardcoded prefix does not impact
            if (namespace.equals(schemaInstanceNS)) {
                hasSchemaInstanceNS = true;
            }
        }
        if (!hasSchemaInstanceNS) {
            root.addNamespaceDeclaration(schemaInstanceNS);
        }
        root.setAttribute("schemaLocation", currentXsdUri + " " + currentXsdLocation, schemaInstanceNS);

        changeNameSpace(root, ns);
    }

    private void changeNameSpace(Element element, Namespace ns) {
        element.setNamespace(ns);
        for (Object childElements : element.getChildren()) {
            changeNameSpace((Element) childElements, ns);
        }
    }
}
