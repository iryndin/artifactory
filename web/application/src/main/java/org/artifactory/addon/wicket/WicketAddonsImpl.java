/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.addon.wicket;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.injection.web.InjectorHolder;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmittingComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.addon.OssAddonsManager;
import org.artifactory.addon.wicket.disabledaddon.AddonNeededBehavior;
import org.artifactory.addon.wicket.disabledaddon.DisabledAddonBehavior;
import org.artifactory.addon.wicket.disabledaddon.DisabledAddonHelpBubble;
import org.artifactory.addon.wicket.disabledaddon.DisabledAddonMenuNode;
import org.artifactory.addon.wicket.disabledaddon.DisabledAddonTab;
import org.artifactory.api.build.BasicBuildInfo;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.VersionInfo;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.md.PropertiesImpl;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.api.version.VersionHolder;
import org.artifactory.api.version.VersionInfoService;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.wicket.ajax.NoAjaxIndicatorDecorator;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.behavior.collapsible.DisabledCollapsibleBehavior;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.CreateUpdatePanel;
import org.artifactory.common.wicket.component.LabeledValue;
import org.artifactory.common.wicket.component.PlaceHolder;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.BaseTitledLink;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.modal.panel.EditValueButtonRefreshBehavior;
import org.artifactory.common.wicket.component.panel.fieldset.FieldSetPanel;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.component.table.columns.BooleanColumn;
import org.artifactory.common.wicket.model.sitemap.MenuNode;
import org.artifactory.common.wicket.property.PropertyItem;
import org.artifactory.common.wicket.util.SetEnableVisitor;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.property.PredefinedValue;
import org.artifactory.descriptor.property.Property;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.group.LdapGroupPopulatorStrategies;
import org.artifactory.descriptor.security.ldap.group.LdapGroupSetting;
import org.artifactory.descriptor.security.sso.CrowdSettings;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.FolderInfo;
import org.artifactory.fs.ItemInfo;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.event.ItemEvent;
import org.artifactory.webapp.servlet.RequestUtils;
import org.artifactory.webapp.wicket.application.ArtifactoryApplication;
import org.artifactory.webapp.wicket.page.base.BasePage;
import org.artifactory.webapp.wicket.page.base.EditProfileLink;
import org.artifactory.webapp.wicket.page.base.LogoutLink;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.BaseBuildsTabPanel;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.actionable.BuildDependencyActionableItem;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.actionable.BuildTabActionableItem;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.maven.PropertiesPanel;
import org.artifactory.webapp.wicket.page.build.actionable.ModuleArtifactActionableItem;
import org.artifactory.webapp.wicket.page.build.actionable.ModuleDependencyActionableItem;
import org.artifactory.webapp.wicket.page.build.tabs.BuildSearchResultsPanel;
import org.artifactory.webapp.wicket.page.build.tabs.DisabledModuleInfoTabPanel;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.page.config.advanced.AdvancedCentralConfigPage;
import org.artifactory.webapp.wicket.page.config.advanced.AdvancedSecurityConfigPage;
import org.artifactory.webapp.wicket.page.config.advanced.MaintenancePage;
import org.artifactory.webapp.wicket.page.config.advanced.SystemInfoPage;
import org.artifactory.webapp.wicket.page.config.general.BaseCustomizingPanel;
import org.artifactory.webapp.wicket.page.config.general.CustomizingPanel;
import org.artifactory.webapp.wicket.page.config.general.GeneralConfigPage;
import org.artifactory.webapp.wicket.page.config.layout.LayoutListPanel;
import org.artifactory.webapp.wicket.page.config.layout.RepoLayoutPage;
import org.artifactory.webapp.wicket.page.config.license.LicensePage;
import org.artifactory.webapp.wicket.page.config.mail.MailConfigPage;
import org.artifactory.webapp.wicket.page.config.proxy.ProxyConfigPage;
import org.artifactory.webapp.wicket.page.config.repos.RepositoryConfigPage;
import org.artifactory.webapp.wicket.page.config.security.LdapGroupListPanel;
import org.artifactory.webapp.wicket.page.config.security.LdapsListPage;
import org.artifactory.webapp.wicket.page.config.security.LdapsListPanel;
import org.artifactory.webapp.wicket.page.config.security.general.SecurityGeneralConfigPage;
import org.artifactory.webapp.wicket.page.config.services.BackupsListPage;
import org.artifactory.webapp.wicket.page.config.services.IndexerConfigPage;
import org.artifactory.webapp.wicket.page.home.HomePage;
import org.artifactory.webapp.wicket.page.home.addon.AddonsInfoPanel;
import org.artifactory.webapp.wicket.page.importexport.repos.ImportExportReposPage;
import org.artifactory.webapp.wicket.page.importexport.system.ImportExportSystemPage;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;
import org.artifactory.webapp.wicket.page.search.ArtifactSaveSearchResultsPanel;
import org.artifactory.webapp.wicket.page.search.LimitlessCapableSearcher;
import org.artifactory.webapp.wicket.page.search.SaveSearchResultsPanel;
import org.artifactory.webapp.wicket.page.security.acl.AclsPage;
import org.artifactory.webapp.wicket.page.security.group.GroupsPage;
import org.artifactory.webapp.wicket.page.security.user.UsersPage;
import org.artifactory.webapp.wicket.panel.export.ExportResultsPanel;
import org.artifactory.webapp.wicket.panel.tabbed.tab.BaseTab;
import org.artifactory.webapp.wicket.util.validation.UriValidator;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildFileBean;
import org.jfrog.build.api.Module;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static org.artifactory.addon.wicket.AddonType.*;

/**
 * Default implementation of the addons interface. Represents a normal execution of artifactory.
 * <p/>
 * <strong>NOTE!</strong> Do not create annonymous or inner classes in addon
 *
 * @author freds
 * @author Yossi Shaul
 */
@org.springframework.stereotype.Component
public final class WicketAddonsImpl implements CoreAddons, WebApplicationAddon, PropertiesWebAddon, SearchAddon,
        WatchAddon, WebstartWebAddon, SsoAddon, LdapGroupWebAddon, BuildAddon, LicensesWebAddon, LayoutsWebAddon {

    @Autowired
    private CentralConfigService centralConfigService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private VersionInfoService versionInfoService;

    @Autowired
    private AuthorizationService authorizationService;

    public String getPageTitle(BasePage page) {
        String serverName = getCentralConfig().getServerName();
        return "Artifactory@" + serverName + " :: " + page.getPageName();
    }

    public MenuNode getSecurityMenuNode(WebstartWebAddon webstartAddon, SsoAddon ssoAddon) {
        MenuNode security = new MenuNode("Security");
        security.addChild(new MenuNode("General", SecurityGeneralConfigPage.class));
        security.addChild(new MenuNode("Users", UsersPage.class));
        security.addChild(new MenuNode("Groups", GroupsPage.class));
        security.addChild(new MenuNode("Permissions", AclsPage.class));
        security.addChild(new MenuNode("LDAP Settings", LdapsListPage.class));
        security.addChild(ssoAddon.getCrowdAddonMenuNode("Crowd Integration"));
        security.addChild(webstartAddon.getKeyPairMenuNode());
        security.addChild(ssoAddon.getHttpSsoMenuNode("HTTP SSO"));
        return security;
    }

    public MenuNode getConfigurationMenuNode(PropertiesWebAddon propertiesWebAddon, LicensesWebAddon licensesWebAddon) {
        MenuNode adminConfiguration = new MenuNode("Configuration");
        adminConfiguration.addChild(new MenuNode("General", GeneralConfigPage.class));
        adminConfiguration.addChild(new MenuNode("Repositories", RepositoryConfigPage.class));
        adminConfiguration.addChild(new MenuNode("Repository Layouts", RepoLayoutPage.class));
        LicensesWebAddon licensesAddon = addonsManager.addonByType(LicensesWebAddon.class);
        adminConfiguration.addChild(licensesAddon.getLicensesMenuNode("Licenses"));
        adminConfiguration.addChild(propertiesWebAddon.getPropertySetsPage("Property Sets"));
        adminConfiguration.addChild(new MenuNode("Proxies", ProxyConfigPage.class));
        adminConfiguration.addChild(new MenuNode("Mail", MailConfigPage.class));
        if (!(addonsManager instanceof OssAddonsManager)) {
            adminConfiguration.addChild(new MenuNode("Register Pro", LicensePage.class));
        }
        return adminConfiguration;
    }

    public Label getUrlBaseLabel(String id) {
        return new Label(id, "Custom URL Base");
    }

    public TextField getUrlBaseTextField(String id) {
        TextField<String> urlBaseTextField = new TextField<String>(id);
        urlBaseTextField.add(new UriValidator("http", "https"));
        return urlBaseTextField;
    }

    public HelpBubble getUrlBaseHelpBubble(String id) {
        return new SchemaHelpBubble(id);
    }

    public IFormSubmittingComponent getLoginLink(String wicketId, Form form) {
        return new DefaultLoginLink(wicketId, "Log In", form);
    }

    public LogoutLink getLogoutLink(String wicketId) {
        return new LogoutLink(wicketId, "Log Out");
    }

    public AbstractLink getProfileLink(String wicketId) {
        return new EditProfileLink("profilePage");
    }

    public String resetPassword(String userName, String remoteAddress, String resetPageUrl) {
        return userGroupService.resetPassword(userName, remoteAddress, resetPageUrl);
    }

    public boolean isInstantiationAuthorized(Class componentClass) {
        return true;
    }

    public Label getUptimeLabel(String wicketId) {
        long uptime = ContextHelper.get().getUptime();
        String uptimeStr = DurationFormatUtils.formatDuration(uptime, "d'd' H'h' m'm' s's'");
        Label uptimeLabel = new Label(wicketId, uptimeStr);
        //Only show uptime for admins
        if (!authorizationService.isAdmin()) {
            uptimeLabel.setVisible(false);
        }
        return uptimeLabel;
    }

    public void addVersionInfo(WebMarkupContainer container, Map<String, String> headersMap) {
        WebMarkupContainer versioningInfo = new WebMarkupContainer("versioningInfo");

        versioningInfo.add(new Label("currentLabel", ConstantValues.artifactoryVersion.getString()));

        Label latestLabel = new Label("latestLabel", "");
        latestLabel.setEscapeModelStrings(false);   // to include a link easily...
        latestLabel.setOutputMarkupId(true);
        String latestWikiUrl = VersionHolder.VERSION_UNAVAILABLE.getWikiUrl();
        CentralConfigDescriptor configDescriptor = centralConfigService.getDescriptor();
        if (!configDescriptor.isOfflineMode()) {
            // try to get the latest version from the cache with a non-blocking call
            VersionHolder latestVersion = versionInfoService.getLatestVersion(headersMap, true);
            latestWikiUrl = latestVersion.getWikiUrl();
            if (VersionInfoService.SERVICE_UNAVAILABLE.equals(latestVersion.getVersion())) {
                // send ajax refresh in 5 second and update the latest version with the result
                latestLabel.add(new UpdateNewsFromCache());
            } else {
                latestLabel.setDefaultModelObject(buildLatestVersionLabel(latestVersion));
            }
        }
        versioningInfo.add(latestLabel);

        ExternalLink wikiLink = new ExternalLink("wikiLink", latestWikiUrl);
        versioningInfo.add(wikiLink);

        container.add(versioningInfo);
    }

    public SaveSearchResultsPanel getSaveSearchResultsPanel(String wicketId, IModel model,
            LimitlessCapableSearcher limitlessCapableSearcher) {
        SaveSearchResultsPanel panel = new ArtifactSaveSearchResultsPanel(wicketId, model, SEARCH);
        panel.setEnabled(false);
        return panel;
    }

    public SaveSearchResultsPanel getBuildSearchResultsPanel(AddonType requestingAddon, Build build) {
        return new BuildSearchResultsPanel(requestingAddon, build);
    }

    public org.artifactory.fs.FileInfo getBuildFileBeanInfo(String buildName, String buildNumber, BuildFileBean bean) {
        return null;
    }

    public void renameBuildNameProperty(String from,
            String to) {
    }

    public void discardOldBuildsByDate(String buildName, Date minimumBuildDate) {
        // nop
    }

    public void discardOldBuildsByCount(String buildName, int count) {
        // nop
    }

    public String getSearchLimitDisclaimer() {
        return StringUtils.EMPTY;
    }

    public ITab getPropertiesTabPanel(ItemInfo itemInfo) {
        return new DisabledAddonTab(Model.of("Properties"), PROPERTIES);
    }

    public ITab getSearchPropertiesTabPanel(FolderInfo folderInfo, List<FileInfo> searchResults) {
        return getPropertiesTabPanel(folderInfo);
    }

    public MenuNode getPropertySearchMenuNode(String nodeTitle) {
        return new DisabledAddonMenuNode(nodeTitle, PROPERTIES);
    }

    public ITab getPropertySearchTabPanel(Page parent, String tabTitle) {
        return new DisabledAddonTab(Model.of(tabTitle), PROPERTIES);
    }

    public MenuNode getPropertySetsPage(String nodeTitle) {
        return new DisabledAddonMenuNode(nodeTitle, PROPERTIES);
    }

    public ITab getRepoConfigPropertySetsTab(String tabTitle, RealRepoDescriptor entity,
            List<PropertySet> propertySets) {
        return new DisabledAddonTab(Model.of(tabTitle), PROPERTIES);
    }

    public BaseModalPanel getEditPropertyPanel(EditValueButtonRefreshBehavior refreshBehavior,
            PropertyItem propertyItem, List<PredefinedValue> values) {
        return null;
    }

    public BaseModalPanel getChangeLicensePanel(EditValueButtonRefreshBehavior refreshBehavior, RepoPath path,
            String currentValues) {
        return null;
    }

    public Properties getProperties(RepoPath repoPath) {
        return new PropertiesImpl();
    }

    public Map<RepoPath, Properties> getProperties(Set<RepoPath> repoPaths) {
        return Maps.newHashMap();
    }

    public void addProperty(RepoPath repoPath, PropertySet propertySet, Property property, String... values) {
    }

    public Component getTreeItemPropertiesPanel(String panelId, ItemInfo itemInfo) {
        return new DisabledPropertiesPanel(panelId, panelId);
    }

    public WebMarkupContainer getKeyPairContainer(String wicketId, String virtualRepoKey, boolean isCreate) {
        WebMarkupContainer container = new WebMarkupContainer(wicketId);
        DropDownChoice<Object> keyPairDropDown = new DropDownChoice<Object>("keyPair", Collections.emptyList());
        keyPairDropDown.setEnabled(false);
        keyPairDropDown.add(new DisabledAddonBehavior(WEBSTART));
        container.add(keyPairDropDown);
        container.add(new WebMarkupContainer("keyPairMessage"));
        container.add(new DisabledAddonHelpBubble("keyPair.help", WEBSTART));
        return container;
    }

    public ItemAction getWatchAction(RepoPath itemRepoPath) {
        ItemAction action = new NopAction();
        action.setEnabled(false);

        return action;
    }

    public ITab getWatchersTab(String tabTitle, RepoPath repoPath) {
        return new DisabledAddonTab(Model.of(tabTitle), WATCH);
    }

    public MarkupContainer getWatchingSinceLabel(String labelId, RepoPath itemRepoPath) {
        return new PlaceHolder(labelId);
    }

    public MarkupContainer getDirectlyWatchedPathPanel(String panelId, RepoPath itemRepoPath) {
        return new PlaceHolder(panelId);
    }

    public MenuNode getHttpSsoMenuNode(String nodeName) {
        return new DisabledAddonMenuNode(nodeName, AddonType.SSO);
    }

    public boolean isCrowdAuthenticationSupported(Class<?> authentication) {
        return false;
    }

    public Authentication authenticateCrowd(Authentication authentication) {
        throw new UnsupportedOperationException("This feature requires the Crowd SSO addon.");
    }

    public MenuNode getCrowdAddonMenuNode(String nodeName) {
        return new DisabledAddonMenuNode(nodeName, AddonType.SSO);
    }

    public void testCrowdConnection(CrowdSettings crowdSettings) throws Exception {
        throw new UnsupportedOperationException("This feature requires the Crowd SSO addon.");
    }

    public void logOffSso(HttpServletRequest request, HttpServletResponse response) {
    }

    public Set findCrowdGroups(String username, CrowdSettings currentCrowdSettings) {
        return Sets.newHashSet();
    }

    public boolean findUser(String userName) {
        return false;
    }

    public void addExternalGroups(String userName, Set<UserInfo.UserGroupInfo> groups) {
        // nop
    }

    public FieldSetPanel getExportResultPanel(String panelId, ActionableItem item) {
        return new ExportResultsPanel(panelId, item);
    }

    public BaseCustomizingPanel getCustomizingPanel(String id, IModel model) {
        return new CustomizingPanel(id, model);
    }

    public WebMarkupContainer getAddonsInfoPanel(String panelId) {
        List<String> installedAddonNames = addonsManager.getInstalledAddonNames();
        List<String> enabledAddonNames = addonsManager.getEnabledAddonNames();
        return new AddonsInfoPanel(panelId, installedAddonNames, enabledAddonNames);
    }

    public ITab getBuildsTab(final RepoAwareActionableItem item) {
        return new DisabledBuildsTab(item);
    }

    public ITab getModuleInfoTab(String buildName, String buildNumber, final Module module) {
        return new DisabledPublishedTab();
    }

    public String getDeleteItemWarningMessage(org.artifactory.fs.ItemInfo item, String defaultMessage) {
        return defaultMessage;
    }

    public String getDeleteVersionsWarningMessage(List<RepoPath> versionPaths, String defaultMessage) {
        return defaultMessage;
    }

    public Set<FileInfo> getArtifactFileInfo(Build build) {
        return Sets.newHashSet();
    }

    public CreateUpdatePanel<LdapGroupSetting> getLdapGroupPanel(CreateUpdateAction createUpdateAction,
            LdapGroupSetting ldapGroupSetting, LdapGroupListPanel ldapGroupListPanel) {
        return null;
    }

    public BooleanColumn<GroupInfo> addExternalGroupIndicator(MultiStatusHolder statusHolder) {
        return null;
    }

    public TitledPanel getLdapGroupConfigurationPanel(String id) {
        return new DisabledLdapGroupListPanel("ldapGroupListPanel");
    }

    public int importExternalGroupsToArtifactory(List ldapGroups, LdapGroupPopulatorStrategies strategy) {
        return 0;
    }

    public Set refreshLdapGroupList(String userName, LdapGroupSetting ldapGroupSetting,
            MultiStatusHolder statusHolder) {
        return Sets.newHashSet();
    }

    public Label getLdapActiveWarning(String wicketId) {
        Label warningLabel = new Label(wicketId);
        warningLabel.setVisible(false);
        return warningLabel;
    }

    public WebMarkupContainer getLdapListPanel(String wicketId) {
        return new LdapsListPanel(wicketId);
    }

    public void saveLdapSetting(MutableCentralConfigDescriptor configDescriptor, LdapSetting ldapSetting) {
        SecurityDescriptor securityDescriptor = configDescriptor.getSecurity();
        if (ldapSetting.isEnabled()) {
            List<LdapSetting> ldapSettings = securityDescriptor.getLdapSettings();
            for (LdapSetting setting : ldapSettings) {
                if (!ldapSetting.equals(setting)) {
                    setting.setEnabled(false);
                }
            }
        }
        LdapSetting setting = securityDescriptor.getLdapSettings(ldapSetting.getKey());
        if (setting != null) {
            List<LdapSetting> ldapSettings = securityDescriptor.getLdapSettings();
            int indexOfLdapSetting = ldapSettings.indexOf(ldapSetting);
            if (indexOfLdapSetting != -1) {
                ldapSettings.set(indexOfLdapSetting, ldapSetting);
            }
        }
    }


    public Set<org.artifactory.fs.FileInfo> getDependencyFileInfo(Build build, Set<String> scopes) {
        return Sets.newHashSet();
    }

    public List<ModuleArtifactActionableItem> getModuleArtifactActionableItems(String buildName, String buildNumber,
            List<Artifact> artifacts) {
        return Lists.newArrayList();
    }

    public List<ModuleDependencyActionableItem> populateModuleDependencyActionableItem(String buildName,
            String buildNumber,
            List<ModuleDependencyActionableItem> dependencies) {
        return Lists.newArrayList();
    }

    public Class<? extends Page> getHomePage() {
        return HomePage.class;
    }

    public MenuNode getAdvancedMenuNode() {
        MenuNode advancedConfiguration = new MenuNode("Advanced");
        advancedConfiguration.addChild(new MenuNode("System Info", SystemInfoPage.class));
        advancedConfiguration.addChild(new MenuNode("System Logs", SystemLogsPage.class));
        advancedConfiguration.addChild(new MenuNode("Maintenance", MaintenancePage.class));
        advancedConfiguration.addChild(new MenuNode("Config Descriptor", AdvancedCentralConfigPage.class));
        advancedConfiguration.addChild(new MenuNode("Security Descriptor", AdvancedSecurityConfigPage.class));
        return advancedConfiguration;
    }

    public MenuNode getBrowserSearchMenuNode() {
        return new DisabledAddonMenuNode("Search Results", SEARCH);
    }

    public MenuNode getImportExportMenuNode() {
        MenuNode adminImportExport = new MenuNode("Import & Export");
        adminImportExport.addChild(new MenuNode("Repositories", ImportExportReposPage.class));
        adminImportExport.addChild(new MenuNode("System", ImportExportSystemPage.class));
        return adminImportExport;
    }

    public MenuNode getKeyPairMenuNode() {
        return new DisabledAddonMenuNode("Key-Pairs", WEBSTART);
    }

    public MenuNode getServicesMenuNode() {
        MenuNode services = new MenuNode("Services");
        services.addChild(new MenuNode("Backups", BackupsListPage.class));
        services.addChild(new MenuNode("Indexer", IndexerConfigPage.class));
        return services;
    }

    public MenuNode getLicensesMenuNode(String nodeName) {
        return new DisabledAddonMenuNode(nodeName, AddonType.LICENSES);
    }

    public ITab getLicensesInfoTab(String title, Build build, boolean hasDeployOnLocal) {
        return new DisabledAddonTab(Model.of(title), AddonType.LICENSES);
    }

    public LabeledValue getLicenseLabel(String id, RepoPath repoPath) {
        LabeledValue label = new LabeledValue(id, "");
        label.setVisible(false);
        return label;
    }

    public AbstractLink getEditLicenseLink(String id, RepoPath path, String currentValues,
            LabeledValue licensesLabel) {
        return getInvisibleLink(id);
    }

    public AbstractLink getAddLicenseLink(String id, RepoPath path, String currentValues,
            LabeledValue licensesLabel) {
        return getInvisibleLink(id);
    }

    public AbstractLink getDeleteLink(String id, RepoPath path, String currentValues, FieldSetBorder border) {
        return getInvisibleLink(id);
    }

    public String getCompanyLogoUrl() {
        String descriptorLogo = centralConfigService.getDescriptor().getLogo();
        if (StringUtils.isNotBlank(descriptorLogo)) {
            return descriptorLogo;
        }

        final ArtifactoryApplication application = ArtifactoryApplication.get();
        if (application.isLogoExists()) {
            return WicketUtils.getWicketAppPath() + "logo?" + application.getLogoModifyTime();
        }

        return null;
    }

    public String getSearchResultsPageAbsolutePath(String resultToSelect) {
        return new StringBuilder(RequestUtils.getWicketServletContextUrl()).append("/").
                append(RequestUtils.WEBAPP_URL_PATH_PREFIX).toString();
    }

    public String getVersionInfo() {
        VersionInfo versionInfo = getCentralConfig().getVersionInfo();
        String product = addonsManager.getProductName();
        return format("%s %s (rev. %s)", product, versionInfo.getVersion(), versionInfo.getRevision());
    }

    public boolean isDefault() {
        return true;
    }

    public boolean isCreateDefaultAdminAccountAllowed() {
        return true;
    }

    public boolean isAolDashboardAdmin(String username, String address) {
        return false;
    }

    private CentralConfigService getCentralConfig() {
        return ArtifactoryApplication.get().getCentralConfig();
    }

    public void addLayoutCopyLink(List<AbstractLink> links, RepoLayout layoutToCopy, String linkId, String linkTitle,
            LayoutListPanel layoutListPanel) {
    }

    public AbstractLink getNewLayoutItemLink(String linkId, String linkTitle, LayoutListPanel layoutListPanel) {
        BaseTitledLink baseTitledLink = new BaseTitledLink(linkId, linkTitle);
        baseTitledLink.add(new CssClass("button-disabled"));
        baseTitledLink.add(new DisabledAddonBehavior(AddonType.LAYOUTS));
        baseTitledLink.add(new CssClass("green-button"));
        return baseTitledLink;
    }

    private BaseTitledLink getInvisibleLink(String id) {
        BaseTitledLink link = new BaseTitledLink(id);
        link.setVisible(false);
        return link;
    }

    private static class UpdateNewsFromCache extends AbstractAjaxTimerBehavior {

        @SpringBean
        private VersionInfoService versionInfoService;

        public UpdateNewsFromCache() {
            super(Duration.seconds(5));
            InjectorHolder.getInjector().inject(this);
        }

        @Override
        protected IAjaxCallDecorator getAjaxCallDecorator() {
            return new NoAjaxIndicatorDecorator();
        }

        @Override
        protected void onTimer(AjaxRequestTarget target) {
            stop(); // try only once
            VersionHolder latestVersion = versionInfoService.getLatestVersionFromCache(true);
            if (!VersionInfoService.SERVICE_UNAVAILABLE.equals(latestVersion.getVersion())) {
                getComponent().setDefaultModelObject(buildLatestVersionLabel(latestVersion));
                target.addComponent(getComponent());
            }
        }
    }

    private static class NopAction extends ItemAction {
        public NopAction() {
            super("");
        }

        @Override
        public void onAction(ItemEvent e) {
        }
    }

    private static class DisabledBuildsTab extends BaseTab {
        private final RepoAwareActionableItem item;

        public DisabledBuildsTab(RepoAwareActionableItem item) {
            super("Builds");
            this.item = item;
        }

        @Override
        public Panel getPanel(String panelId) {
            return new DisabledBuildsTabPanel(panelId, item);
        }

        @Override
        public void onNewTabItem(Loop.LoopItem item) {
            super.onNewTabItem(item);
            item.add(new AddonNeededBehavior(AddonType.BUILD));
        }
    }

    private static class DisabledPublishedTab extends BaseTab {
        public DisabledPublishedTab() {
            super("Published Modules");
        }

        @Override
        public Panel getPanel(String panelId) {
            return new DisabledModuleInfoTabPanel(panelId);
        }

        @Override
        public void onNewTabItem(Loop.LoopItem item) {
            super.onNewTabItem(item);
            item.add(new AddonNeededBehavior(AddonType.BUILD));
        }
    }

    private static class DisabledLdapGroupListPanel extends LdapGroupListPanel {
        public DisabledLdapGroupListPanel(String id) {
            super(id);
            add(new CssClass("disabled-panel"));
            add(new AddonNeededBehavior(AddonType.LDAP).setPosition("above", "below"));
            disableAll(this);
        }
    }

    private static class DisabledBuildsTabPanel extends BaseBuildsTabPanel {

        /**
         * Main constructor
         *
         * @param id   ID to assign to the panel
         * @param item Selected repo item
         */
        public DisabledBuildsTabPanel(String id, RepoAwareActionableItem item) {
            super(id, item);
            add(new CssClass("disabled-panel"));

            disableAll(this);
        }

        @Override
        protected List<BasicBuildInfo> getArtifactBuilds() {
            return Lists.newArrayList();
        }

        @Override
        protected List<BasicBuildInfo> getDependencyBuilds() {
            return Lists.newArrayList();
        }

        @Override
        protected List<BuildTabActionableItem> getArtifactActionableItems(BasicBuildInfo basicInfo) {
            return Lists.newArrayList();
        }

        @Override
        protected List<BuildDependencyActionableItem> getDependencyActionableItems(BasicBuildInfo basicInfo) {
            return Lists.newArrayList();
        }
    }

    private static String buildLatestVersionLabel(VersionHolder latestVersion) {
        return String.format("(latest release is <a href=\"%s\" target=\"_blank\">%s</a>)",
                latestVersion.getDownloadUrl(), latestVersion.getVersion());
    }

    private static void disableAll(MarkupContainer container) {
        container.setEnabled(false);
        container.visitChildren(new SetEnableVisitor<Component>(false));
    }

    private static class DisabledPropertiesPanel extends PropertiesPanel {

        public DisabledPropertiesPanel(String id, String nestedPanelId) {
            super(id);
            add(new DisabledCollapsibleBehavior());
            add(new WebMarkupContainer(nestedPanelId));
        }

        @Override
        protected Component newToolbar(String id) {
            return new DisabledAddonHelpBubble(id, PROPERTIES);
        }
    }
}
