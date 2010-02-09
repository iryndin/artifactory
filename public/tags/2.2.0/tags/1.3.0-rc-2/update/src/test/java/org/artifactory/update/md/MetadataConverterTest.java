package org.artifactory.update.md;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.IOUtils;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.stat.StatsInfo;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.testng.annotations.BeforeClass;

import java.io.InputStream;

/**
 * Base class for the matadata converters tests.
 *
 * @author Yossi Shaul
 */
public class MetadataConverterTest {
    protected XStream xstream;

    @BeforeClass
    public void setup() {
        xstream = new XStream();
        xstream.processAnnotations(new Class[]{FileInfo.class, StatsInfo.class, FolderInfo.class});
    }

    protected Document convertMetadata(String fileMetadata, MetadataConverter converter) throws Exception {
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
