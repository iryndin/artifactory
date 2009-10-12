package org.artifactory.webapp.wicket.common.component.panel.actionable.general;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.servlet.RequestUtils;
import org.artifactory.webapp.wicket.common.component.TextContentPanel;
import org.artifactory.webapp.wicket.common.component.panel.fieldset.FieldSetPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * Displays distribution management section for users to add the the pom in order to diploy to a selected local/cached
 * repository.
 *
 * @author Yossi Shaul
 */
public class DistributionManagementPanel extends FieldSetPanel {
    private static final Logger log = LoggerFactory.getLogger(DistributionManagementPanel.class);
    private String firstIndention = "";
    private String secondIndention = "    ";
    private boolean isCache = false;

    public DistributionManagementPanel(String id, RepoAwareActionableItem repoItem) {
        super(id);
        addDistributionManagement(repoItem);
    }

    @Override
    public String getTitle() {
        if (isCache) {
            return "Repository Reference";
        }
        return "Distribution Management";
    }

    private void addDistributionManagement(RepoAwareActionableItem repoItem) {
        LocalRepoDescriptor repo = repoItem.getRepo();
        CentralConfigService cc = ContextHelper.get().getCentralConfig();
        String id = cc.getServerName();
        String repoUrl = buildRepoUrl(repo);
        isCache = repo.isCache();
        StringBuilder sb = new StringBuilder();
        setIndentions(isCache);
        if (!isCache) {
            sb.append("<distributionManagement>\n");
        }
        if (repo.isHandleReleases()) {
            sb.append(firstIndention).append("<repository>\n")
                    .append(secondIndention).append("<id>").append(id).append("-releases</id>\n")
                    .append(secondIndention).append("<name>").append(id).append("-releases</name>\n")
                    .append(secondIndention).append("<url>").append(repoUrl).append("</url>\n")
                    .append(firstIndention).append("</repository>\n");
        }

        if (repo.isHandleSnapshots()) {
            sb.append(firstIndention).append("<snapshotRepository>\n")
                    .append(secondIndention).append("<id>").append(id).append("-snapshots</id>\n")
                    .append(secondIndention).append("<name>").append(id).append("-snapshots</name>\n")
                    .append(secondIndention).append("<url>").append(repoUrl).append("</url>\n")
                    .append(firstIndention).append("</snapshotRepository>\n");
        }
        if (!isCache) {
            sb.append("</distributionManagement>");
        }
        add(new TextContentPanel("ditributionManagamentContainer").setContent(sb.toString()));

        log.debug("Pom definition: {}", sb);
    }

    private String buildRepoUrl(LocalRepoDescriptor repo) {
        ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
        HttpServletRequest request = servletWebRequest.getHttpServletRequest();
        String servletContextUrl = RequestUtils.getServletContextUrl(request);
        StringBuilder sb = new StringBuilder();
        if (repo instanceof LocalCacheRepoDescriptor) {
            RemoteRepoDescriptor remoteRepoDescriptor = ((LocalCacheRepoDescriptor) repo).getRemoteRepo();
            if (remoteRepoDescriptor != null) {
                sb.append(servletContextUrl).append("/").append(remoteRepoDescriptor.getKey());
            } else {
                String fixedKey = StringUtils.remove(repo.getKey(), "-cache");
                sb.append(servletContextUrl).append("/").append(fixedKey);
            }
        } else {
            sb.append(servletContextUrl).append("/").append(repo.getKey());
        }
        return sb.toString();
    }


    private void setIndentions(boolean isCache) {
        if (!isCache) {
            firstIndention += "    ";
            secondIndention += "    ";
        }
    }
}