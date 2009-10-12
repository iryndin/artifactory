package org.artifactory.update;

import org.apache.commons.io.IOUtils;
import org.artifactory.version.converter.XmlConverter;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;

import java.io.InputStream;

/**
 * Base class for the xml converters (security, metadata, etc.)
 *
 * @author Yossi Shaul
 */
public abstract class XmlConverterTest {
    protected Document convertMetadata(String fileMetadata, XmlConverter converter) throws Exception {
        InputStream is = loadResource(fileMetadata);
        Document doc = buildDocFromFile(is);
        converter.convert(doc);
        return doc;
    }

    protected InputStream loadResource(String path) {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalArgumentException("Cannot find resource " + path);
        }
        return is;
    }

    protected Document buildDocFromFile(InputStream is) throws Exception {
        SAXBuilder sb = new SAXBuilder();
        try {
            return sb.build(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
