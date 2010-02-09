package org.artifactory.webapp.wicket.page.home;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.ArtifactCount;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.version.VersionInfoService;
import static org.artifactory.common.ConstantsValue.artifactoryVersion;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.webapp.wicket.application.ArtifactoryWebSession;
import org.artifactory.webapp.wicket.common.component.SimplePageLink;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.page.browse.treebrowser.BrowseRepoPage;
import org.artifactory.webapp.wicket.page.security.login.LoginPage;
import org.artifactory.webapp.wicket.page.security.login.LogoutPage;
import org.artifactory.webapp.wicket.page.security.profile.ProfilePage;
import org.artifactory.webapp.wicket.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Yoav Aharoni
 */
public class WelcomeBorder extends TitledBorder {
    private static final Logger LOG = LoggerFactory.getLogger(WelcomeBorder.class);
    private static final String LATEST_VERSION_NA = "NA";

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
        addLatestVersionInfo();
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

        SimplePageLink loginLink = new SimplePageLink("loginLink", "log in", LoginPage.class);
        add(loginLink);

        SimplePageLink logoutLink = new SimplePageLink("logoutLink", "log out", LogoutPage.class);
        add(logoutLink);

        // update profile link
        SimplePageLink profileLink = new SimplePageLink(
                "profileLink", authorizationService.currentUsername(), ProfilePage.class) {
            @Override
            public boolean isEnabled() {
                return authorizationService.isUpdatableProfile();
            }
        };
        add(profileLink);

        if (ArtifactoryWebSession.get().isSignedIn() && !authorizationService.isAnonymous()) {
            loginLink.setVisible(false);
        } else {
            logoutLink.setVisible(false);
        }
    }

    private void addLatestVersionInfo() {
        Label currentLabel = new Label("currentLabel", artifactoryVersion.getString());
        add(currentLabel);

        Label latestLabel = new Label("latestLabel", "");
        latestLabel.setVisible(false);
        add(latestLabel);

        CentralConfigDescriptor configDescriptor = centralConfigService.getDescriptor();
        if (!configDescriptor.isOfflineMode()) {
            Map<String, String> headersMap = WebUtils.getHeadersMap();
            String latestVersion = versionInfoService.getLatestVersion(headersMap, true);
            if (!LATEST_VERSION_NA.equals(latestVersion)) {
                latestLabel.setModelObject(latestVersion);
                latestLabel.setVisible(true);
            }
        }
    }
}
