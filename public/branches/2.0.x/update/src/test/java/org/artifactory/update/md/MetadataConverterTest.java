package org.artifactory.update.md;

import com.thoughtworks.xstream.XStream;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.update.XmlConverterTest;
import org.testng.annotations.BeforeClass;

/**
 * Base class for the matadata converters tests.
 *
 * @author Yossi Shaul
 */
public abstract class MetadataConverterTest extends XmlConverterTest {
    protected XStream xstream;

    @BeforeClass
    public void setup() {
        xstream = new XStream();
        xstream.processAnnotations(new Class[]{FileInfo.class, StatsInfo.class, FolderInfo.class});
    }

}
