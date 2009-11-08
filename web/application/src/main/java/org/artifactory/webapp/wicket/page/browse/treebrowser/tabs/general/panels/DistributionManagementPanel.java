/*
 * This file is part of Artifactory.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general.panels;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.wicket.component.TextContentPanel;
import org.artifactory.common.wicket.component.panel.fieldset.FieldSetPanel;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.HttpUtils;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * Displays distribution management section for users to add the the pom in order to deploy to a selected local/cached
 * repository.
 *
 * @author Yossi Shaul
 */
public class DistributionManagementPanel extends FieldSetPanel {
    private static final Logger log = LoggerFactory.getLogger(DistributionManagementPanel.class);
    private String firstIndention = "";
    private String secondIndention = "    ";
    private boolean isCache = false;
    private StringBuilder sb = new StringBuilder();

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
        sb.delete(0, sb.length());
        LocalRepoDescriptor repo = repoItem.getRepo();
        CentralConfigService cc = ContextHelper.get().getCentralConfig();
        String id = cc.getServerName();
        String repoUrl = buildRepoUrl(repo);
        isCache = repo.isCache();
        setIndentions(isCache);
        if (!isCache) {
            appendEsc("<distributionManagement>\n");
        }
        if (repo.isHandleReleases()) {
            appendEsc(firstIndention);
            appendEsc("<repository>\n");
            appendEsc(secondIndention);
            appendEsc("<id>");
            appendEsc(id);
            appendEsc("</id>\n");
            appendEsc(secondIndention);
            appendEsc("<name>");
            appendEsc(id);
            appendEsc("-releases</name>\n");
            appendEsc(secondIndention);
            appendEsc("<url>");
            append("<a href=\"");
            append(repoUrl);
            append("\">");
            append(repoUrl);
            append("</a>");
            appendEsc("</url>\n");
            appendEsc(firstIndention);
            appendEsc("</repository>\n");
        }

        if (repo.isHandleSnapshots()) {
            appendEsc(firstIndention);
            appendEsc("<snapshotRepository>\n");
            appendEsc(secondIndention);
            appendEsc("<id>");
            appendEsc(id);
            appendEsc("</id>\n");
            appendEsc(secondIndention);
            appendEsc("<name>");
            appendEsc(id);
            appendEsc("-snapshots</name>\n");
            appendEsc(secondIndention);
            appendEsc("<url>");
            append("<a href=\"");
            append(repoUrl);
            append("\">");
            append(repoUrl);
            append("</a>");
            appendEsc("</url>\n");
            appendEsc(firstIndention);
            appendEsc("</snapshotRepository>\n");
        }
        if (!isCache) {
            appendEsc("</distributionManagement>");
        }
        add(new TextContentPanel("ditributionManagamentContainer", false).setContent(sb.toString()));

        log.debug("Pom definition: {}", sb);
    }

    private void append(String toAppend) {
        sb.append(toAppend);
    }

    private void appendEsc(String toAppend) {
        toAppend = toAppend.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        sb.append(toAppend);
    }

    private String buildRepoUrl(LocalRepoDescriptor repo) {
        ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
        HttpServletRequest request = servletWebRequest.getHttpServletRequest();
        String servletContextUrl = HttpUtils.getServletContextUrl(request);
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