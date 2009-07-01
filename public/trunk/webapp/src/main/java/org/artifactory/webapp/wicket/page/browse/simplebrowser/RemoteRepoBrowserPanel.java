package org.artifactory.webapp.wicket.page.browse.simplebrowser;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.protocol.http.WebRequest;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.webapp.servlet.RequestUtils;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledPanel;
import org.artifactory.webapp.wicket.utils.WebUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * Remote repo simple browsing is not supported. This panel will display a message and a link to the cache repo.
 *
 * @author Yossi Shaul
 */
public class RemoteRepoBrowserPanel extends TitledPanel {

    public RemoteRepoBrowserPanel(String id, RepoPath repoPath) {
        super(id);

        // create a link to the cache of the remote repo
        WebRequest request = WebUtils.getWebRequest();
        HttpServletRequest httpRequest = request.getHttpServletRequest();
        String servletContextUrl = RequestUtils.getServletContextUrl(httpRequest);
        String cacheRepoUrl = servletContextUrl + "/" + repoPath.getRepoKey() + "-cache/";
        add(new ExternalLink("cache-link", cacheRepoUrl));
    }
}
