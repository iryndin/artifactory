package org.artifactory.webapp.wicket.page.home;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.ArtifactCount;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.version.VersionInfoService;
import static org.artifactory.common.ConstantsValue.artifactoryVersion;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.webapp.wicket.common.ajax.NoAjaxIndicatorDecorator;
import org.artifactory.webapp.wicket.common.component.SimplePageLink;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.page.base.EditProfileLink;
import org.artifactory.webapp.wicket.page.base.LoginLink;
import org.artifactory.webapp.wicket.page.base.LogoutLink;
import org.artifactory.webapp.wicket.page.browse.treebrowser.BrowseRepoPage;
import org.artifactory.webapp.wicket.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Yoav Aharoni
 */
public class WelcomeBorder extends TitledBorder {
    private static final Logger LOG = LoggerFactory.getLogger(WelcomeBorder.class);

    @SpringBean
    private RepositoryService repoService;

    @SpringBean
    private AuthorizationService authorizationService;

    @SpringBean
    private VersionInfoService versionInfoService;

    @SpringBean
    private CentralConfigService centralConfigService;

    public WelcomeBorder(String id) {
        super(id);
        addUptime();
        addArtifactsCount();
        addCurrentUserInfo();
        addVersionInfo();
    }

    private void addUptime() {
        long uptime = ContextHelper.get().getUptime();
        String uptimeStr = DurationFormatUtils.formatDuration(uptime, "d'd' H'h' m'm' s's'");
        Label uptimeLabel = new Label("uptime", uptimeStr);
        //Only show uptime for admins
        if (!authorizationService.isAdmin()) {
            uptimeLabel.setVisible(false);
        }
        add(uptimeLabel);
    }

    private void addArtifactsCount() {
        Label countLabel = new Label("artifactsCount", "");
        add(countLabel);
        try {
            ArtifactCount count = repoService.getArtifactCount();
            countLabel.setModelObject(count.getTotalCount());
        } catch (RepositoryRuntimeException e) {
            countLabel.setVisible(false);
            LOG.warn("Failed to retrieve artifacts count: " + e.getMessage());
        }
    }

    private void addCurrentUserInfo() {
        add(new SimplePageLink("browseLink", "browse", BrowseRepoPage.class));

        add(new LoginLink("loginLink", "log in"));
        add(new LogoutLink("logoutLink", "log out"));
        add(new EditProfileLink("profileLink"));
    }

    private void addVersionInfo() {
        Label currentLabel = new Label("currentLabel", artifactoryVersion.getString());
        add(currentLabel);

        final Label latestLabel = new Label("latestLabel", "");
        latestLabel.setOutputMarkupId(true);
        CentralConfigDescriptor configDescriptor = centralConfigService.getDescriptor();
        if (!configDescriptor.isOfflineMode()) {
            // first try to get the latest version from the cache (we don't want to block)
            String latestVersion = versionInfoService.getLatestVersionFromCache(true);
            if (VersionInfoService.SERVICE_UNAVAILABLE.equals(latestVersion)) {
                // send ajax refresh in 1 second and update the latest version with the result
                latestLabel.add(new AbstractAjaxTimerBehavior(Duration.seconds(1)) {
                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new NoAjaxIndicatorDecorator();
                    }

                    @Override
                    protected void onTimer(AjaxRequestTarget target) {
                        stop(); // try only once
                        Map<String, String> headersMap = WebUtils.getHeadersMap();
                        String latestVersion = versionInfoService.getLatestVersion(headersMap, true);
                        if (!VersionInfoService.SERVICE_UNAVAILABLE.equals(latestVersion)) {
                            latestLabel.setModelObject(buildLatestversionString(latestVersion));
                            target.addComponent(latestLabel);
                        }
                    }
                });
            } else {
                latestLabel.setModelObject(buildLatestversionString(latestVersion));
            }
        }
        add(latestLabel);
    }

    private String buildLatestversionString(String latestVersion) {
        return "(latest release is " + latestVersion + ")";
    }
}
