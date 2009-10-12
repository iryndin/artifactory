package org.artifactory.version.converter;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Sets the ntHost element to be the localhost name if the proxy domain is set.
 *
 * @author Yossi Shaul
 */
public class ProxyNTHostConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(ProxyNTHostConverter.class);

    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        Element proxiesElement = root.getChild("proxies", ns);
        if (proxiesElement != null) {
            List proxies = proxiesElement.getChildren("proxy", ns);
            for (Object proxyObj : proxies) {
                Element proxy = (Element) proxyObj;
                Element domain = proxy.getChild("domain", ns);
                if (domain != null) {
                    Element ntHost = new Element("ntHost", ns);
                    ntHost.setText(getHostName());
                    // insert the ntHost element right before the domain element 
                    proxy.addContent(proxy.indexOf(domain), ntHost);
                }
            }
        }
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("Failed to get host name");
            return "unknown";
        }
    }
}
