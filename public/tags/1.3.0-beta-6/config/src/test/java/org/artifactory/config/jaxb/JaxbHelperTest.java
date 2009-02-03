package org.artifactory.config.jaxb;

import org.apache.commons.io.IOUtils;
import org.artifactory.descriptor.config.CentralConfigDescriptorImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Tests the JaxbHelper.
 *
 * @author Noam Tenne
 */
@Test
public class JaxbHelperTest {

    public void onTheFlyConversion() throws Exception {
        InputStream is = getClass().getResourceAsStream("/config/install/config.1.1.0.xml");
        Assert.assertNotNull(is, "Cannot find the test resource");
        File configToConvert = new File("target", "config.1.1.0.xml");
        configToConvert.getParentFile().mkdirs();
        IOUtils.copy(is, new FileOutputStream(configToConvert));

        CentralConfigDescriptorImpl cc = JaxbHelper.readConfig(configToConvert);

        Assert.assertTrue(configToConvert.exists());
        Assert.assertNotNull(cc);
    }
}
