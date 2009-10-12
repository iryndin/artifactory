package org.artifactory.config;

import org.artifactory.config.jaxb.JaxbHelper;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.net.URL;

/**
 * Tests the default configuration works.
 *
 * @author Yossi Shaul
 */
@Test
public class DefaultConfigTest {
    private final static Logger log = LoggerFactory.getLogger(DefaultConfigTest.class);

    public void testReadDefaultConfig() {
        InputStream is = getClass().getResourceAsStream(
                "/META-INF/default/artifactory.config.xml");
        JaxbHelper<CentralConfigDescriptor> helper = new JaxbHelper<CentralConfigDescriptor>();
        URL schemaUrl = getClass().getClassLoader().getResource("artifactory.xsd");

        CentralConfigDescriptor centralConfig = helper.read(is, CentralConfigDescriptorImpl.class,
                schemaUrl);
        log.debug("config = " + centralConfig);
    }
}
