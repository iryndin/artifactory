package org.artifactory.config.xml;

import javax.xml.parsers.SAXParserFactory;

/**
 * User: freds Date: Jun 2, 2008 Time: 7:14:17 PM
 */
public class ArtifactoryXmlFactory {
    private static boolean xmlInitialized;
    private static SAXParserFactory factory;

    private static synchronized void initXmlConfiguration() {
        if (xmlInitialized) {
            return;
        }
        xmlInitialized = true;
        //Create the sax parser factory
        factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        try {
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", true);
            factory.setFeature("http://xml.org/sax/features/resolve-dtd-uris", false);
            factory.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
            factory.setFeature("http://xml.org/sax/features/namespaces", true);
            factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
        } catch (Exception e) {
            factory = null;
            throw new RuntimeException("SAX parser factory initialization error.", e);
        }
    }

    public static SAXParserFactory getFactory() {
        initXmlConfiguration();
        return factory;
    }
}
