package org.artifactory.version.converter.v141;

import org.artifactory.log.LoggerFactory;
import org.artifactory.version.converter.XmlConverter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;

import java.util.List;

/**
 * @author Tomer Cohen
 */
public class ProxyDefaultConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(ProxyDefaultConverter.class);

    public void convert(Document doc) {
        Element root = doc.getRootElement();
        Namespace ns = root.getNamespace();

        Element proxiesElement = root.getChild("proxies", ns);
        if (proxiesElement == null || proxiesElement.getChildren().isEmpty()) {
            log.debug("No proxies found");
            return;
        }
        List proxies = proxiesElement.getChildren();

        Element repositoriesElement = root.getChild("remoteRepositories", ns);
        List remoteRepos = repositoriesElement.getChildren();
        if (remoteRepos == null || remoteRepos.size() == 0) {
            log.debug("No remote repos found");
            return;
        }

        Element defaultCandidate = null;
        for (Object remoteRepoObj : remoteRepos) {
            Element remoteRepo = (Element) remoteRepoObj;
            Element remoteRepoProxy = remoteRepo.getChild("proxyRef", ns);
            if (remoteRepoProxy == null) {
                //If the remote repository does not have a proxy, we can stop right here.
                return;
            }
            if (defaultCandidate != null && !remoteRepoProxy.getText().equals(defaultCandidate.getText())) {
                return;
            }
            if (defaultCandidate == null) {
                defaultCandidate = remoteRepoProxy;
            }
        }

        for (Object proxyObj : proxies) {
            Element proxy = (Element) proxyObj;
            Element proxyKey = proxy.getChild("key", ns);
            if (proxyKey.getText().equals(defaultCandidate.getText())) {
                if (proxy.getChild("defaultProxy", ns) == null) {   // RTFACT-2450
                    Element element = new Element("defaultProxy", ns);
                    element.setText("true");
                    proxy.addContent(element);
                }
                break;
            }
        }
    }
}
