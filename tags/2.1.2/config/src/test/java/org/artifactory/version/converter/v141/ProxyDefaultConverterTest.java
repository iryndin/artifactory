package org.artifactory.version.converter.v141;

import org.artifactory.convert.XmlConverterTest;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @author Tomer Cohen
 */
public class ProxyDefaultConverterTest extends XmlConverterTest {

    @Test
    public void covertRemoteRepositoriesWithDefaultProxy() throws Exception {
        Document doc = convertXml("/config/test/config.1.4.1_default-proxy.xml",
                new ProxyDefaultConverter());
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();
        Element proxies = root.getChild("proxies", ns);
        List proxy = proxies.getChildren();
        Element defaultProxy = (Element) proxy.get(0);
        Element isDefaultProxy = defaultProxy.getChild("defaultProxy", ns);
        Assert.assertNotNull(isDefaultProxy);
        Assert.assertEquals(isDefaultProxy.getText(), "true");
    }

}
