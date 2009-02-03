package org.artifactory.webapp.wicket.common.component.panel.actionable.general;

import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.servlet.RequestUtils;
import org.artifactory.webapp.wicket.common.component.TextContentPanel;
import org.artifactory.webapp.wicket.common.component.panel.fieldset.FieldSetPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * Displays distribution management section for users to add the the pom in order to diploy
 * to a selected local/cached repository.
 *
 * @author Yossi Shaul
 */
public class DistributionManagementPanel extends FieldSetPanel {
    private static final Logger log = LoggerFactory.getLogger(DistributionManagementPanel.class);

    public DistributionManagementPanel(String id, RepoAwareActionableItem repoItem) {
        super(id);
        addDistributionManagement(repoItem);
    }

    @Override
    public String getTitle() {
        return "Distribution Management";
    }

    private void addDistributionManagement(RepoAwareActionableItem repoItem) {
        CentralConfigService cc = ContextHelper.get().getCentralConfig();
        String id = cc.getServerName();
        String repoUrl = buildRepoUrl(repoItem);

        StringBuilder sb = new StringBuilder();
        sb.append("<distributionManagement>\n");
        if (repoItem.getRepo().isHandleReleases()) {
            sb.append("    <repository>\n")
                    .append("        <id>").append(id).append("-releases</id>\n")
                    .append("        <name>").append(id).append("-releases</name>\n")
                    .append("        <url>").append(repoUrl).append("</url>\n")
                    .append("    </repository>\n");
        }

        if (repoItem.getRepo().isHandleSnapshots()) {
            sb.append("    <snapshotRepository>\n")
                    .append("        <id>").append(id).append("-snapshots</id>\n")
                    .append("        <name>").append(id).append("-snapshots</name>\n")
                    .append("        <url>").append(repoUrl).append("</url>\n")
                    .append("    </snapshotRepository>\n");
        }
        sb.append("</distributionManagement>");

        add(new TextContentPanel("ditributionManagamentContainer").setContent(sb.toString()));

        log.debug("Pom definition: {}", sb);
    }


    private String buildRepoUrl(RepoAwareActionableItem repoItem) {
        ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
        HttpServletRequest request = servletWebRequest.getHttpServletRequest();
        String servletContextUrl = RequestUtils.getServletContextUrl(request);
        StringBuilder sb = new StringBuilder();
        sb.append(servletContextUrl).append("/").append(repoItem.getRepo().getKey());
        return sb.toString();
    }
}
