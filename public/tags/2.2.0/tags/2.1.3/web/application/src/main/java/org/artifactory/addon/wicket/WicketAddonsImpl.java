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

package org.artifactory.addon.wicket;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmittingComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import static org.artifactory.addon.wicket.Addon.*;
import org.artifactory.addon.wicket.disabledaddon.AddonNeededBehavior;
import org.artifactory.addon.wicket.disabledaddon.DisabledAddonBehavior;
import org.artifactory.addon.wicket.disabledaddon.DisabledAddonHelpBubble;
import org.artifactory.addon.wicket.disabledaddon.DisabledAddonMenuNode;
import org.artifactory.addon.wicket.disabledaddon.DisabledAddonTab;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.VersionInfo;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.md.Properties;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.version.VersionHolder;
import org.artifactory.api.version.VersionInfoService;
import org.artifactory.build.api.Artifact;
import org.artifactory.build.api.Build;
import org.artifactory.build.api.Dependency;
import org.artifactory.build.api.Module;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.wicket.ajax.NoAjaxIndicatorDecorator;
import org.artifactory.common.wicket.behavior.collapsible.DisabledCollapsibleBehavior;
import org.artifactory.common.wicket.component.PlaceHolder;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.panel.fieldset.FieldSetPanel;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.common.wicket.model.sitemap.MenuNode;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.version.ArtifactoryConfigVersion;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.event.ItemEvent;
import org.artifactory.webapp.wicket.application.ArtifactoryApplication;
import org.artifactory.webapp.wicket.components.container.LogoLinkContainer;
import org.artifactory.webapp.wicket.page.base.BasePage;
import org.artifactory.webapp.wicket.page.base.EditProfileLink;
import org.artifactory.webapp.wicket.page.base.LogoutLink;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.DisabledBuildsTabPanel;
import org.artifactory.webapp.wicket.page.build.actionable.ModuleArtifactActionableItem;
import org.artifactory.webapp.wicket.page.build.actionable.ModuleDependencyActionableItem;
import org.artifactory.webapp.wicket.page.build.tabs.BuildSearchResultsPanel;
import org.artifactory.webapp.wicket.page.build.tabs.DisabledModuleInfoTabPanel;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.page.config.advanced.AdvancedCentralConfigPage;
import org.artifactory.webapp.wicket.page.config.advanced.AdvancedSecurityConfigPage;
import org.artifactory.webapp.wicket.page.config.advanced.MaintenancePage;
import org.artifactory.webapp.wicket.page.config.general.CustomizingPanel;
import org.artifactory.webapp.wicket.page.config.general.GeneralConfigPage;
import org.artifactory.webapp.wicket.page.config.mail.MailConfigPage;
import org.artifactory.webapp.wicket.page.config.proxy.ProxyConfigPage;
import org.artifactory.webapp.wicket.page.config.repos.RepositoryConfigPage;
import org.artifactory.webapp.wicket.page.config.security.LdapsListPage;
import org.artifactory.webapp.wicket.page.config.security.general.SecurityGeneralConfigPage;
import org.artifactory.webapp.wicket.page.config.services.BackupsListPage;
import org.artifactory.webapp.wicket.page.config.services.IndexerConfigPage;
import org.artifactory.webapp.wicket.page.home.HomePage;
import org.artifactory.webapp.wicket.page.home.addon.AddonsInfoPanel;
import org.artifactory.webapp.wicket.page.importexport.repos.ImportExportReposPage;
import org.artifactory.webapp.wicket.page.importexport.system.ImportExportSystemPage;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;
import org.artifactory.webapp.wicket.page.search.LimitlessCapableSearcher;
import org.artifactory.webapp.wicket.page.search.SaveSearchResultsPanel;
import org.artifactory.webapp.wicket.page.security.acl.AclsPage;
import org.artifactory.webapp.wicket.page.security.group.GroupsPage;
import org.artifactory.webapp.wicket.page.security.user.UsersPage;
import org.artifactory.webapp.wicket.panel.export.ExportResultsPanel;
import org.artifactory.webapp.wicket.panel.tabbed.tab.BaseTab;
import org.artifactory.webapp.wicket.util.validation.ServerIdValidator;
import org.artifactory.webapp.wicket.util.validation.UriValidator;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of the addons interface. Represents a normal execution of artifactory.
 *
 * @author freds
 * @author Yossi Shaul
 */
@Component
public final class WicketAddonsImpl implements CoreAddons, WebApplicationAddon, PropertiesAddon, SearchAddon,
        WatchAddon, WebstartWebAddon, HttpSsoAddon, BuildAddon {

    public boolean isDefault() {
        return true;
    }

    public String getVersionInfo() {
        VersionInfo versionInfo = getCentralConfig().getVersionInfo();
        return format("Artifactory %s (rev. %s)", versionInfo.getVersion(), versionInfo.getRevision());
    }

    public String getPageTitle(BasePage page) {
        String serverName = getCentralConfig().getServerName();
        return "Artifactory@" + serverName + " :: " + page.getPageName();
    }

    public MenuNode getImportExportMenuNode() {
        MenuNode adminImportExport = new MenuNode("Import & Export");
        adminImportExport.addChild(new MenuNode("Repositories", ImportExportReposPage.class));
        adminImportExport.addChild(new MenuNode("System", ImportExportSystemPage.class));
        return adminImportExport;
    }

    public MenuNode getSecurityMenuNode(WebstartWebAddon webstartAddon, HttpSsoAddon httpSsoAddon) {
        MenuNode security = new MenuNode("Security");
        security.addChild(new MenuNode("General", SecurityGeneralConfigPage.class));
        security.addChild(new MenuNode("Users", UsersPage.class));
        security.addChild(new MenuNode("Groups", GroupsPage.class));
        security.addChild(new MenuNode("Permissions", AclsPage.class));
        security.addChild(new MenuNode("LDAP Settings", LdapsListPage.class));
        security.addChild(webstartAddon.getKeyPairMenuNode());
        security.addChild(httpSsoAddon.getHttpSsoMenuNode("HTTP SSO"));
        return security;
    }

    public MenuNode getServicesMenuNode() {
        MenuNode services = new MenuNode("Services");
        services.addChild(new MenuNode("Backups", BackupsListPage.class));
        services.addChild(new MenuNode("Indexer", IndexerConfigPage.class));
        return services;
    }

    public MenuNode getConfigurationMenuNode(PropertiesAddon propertiesAddon) {
        MenuNode adminConfiguration = new MenuNode("Configuration");
        adminConfiguration.addChild(new MenuNode("General", GeneralConfigPage.class));
        adminConfiguration.addChild(new MenuNode("Repositories", RepositoryConfigPage.class));
        adminConfiguration.addChild(propertiesAddon.getPropertySetsPage("Property Sets"));
        adminConfiguration.addChild(new MenuNode("Proxies", ProxyConfigPage.class));
        adminConfiguration.addChild(new MenuNode("Mail", MailConfigPage.class));
        return adminConfiguration;
    }

    public MenuNode getAdvancedMenuNode() {
        MenuNode advancedConfiguration = new MenuNode("Advanced");
        advancedConfiguration.addChild(new MenuNode("System Logs", SystemLogsPage.class));
        advancedConfiguration.addChild(new MenuNode("Maintenance", MaintenancePage.class));
        advancedConfiguration.addChild(new MenuNode("Config Descriptor", AdvancedCentralConfigPage.class));
        advancedConfiguration.addChild(new MenuNode("Security Descriptor", AdvancedSecurityConfigPage.class));
        return advancedConfiguration;
    }

    public Label getUrlBaseLabel(String id) {
        return new Label(id, "Custom URL Base");
    }

    public TextField getUrlBaseTextField(String id) {
        TextField urlBaseTextField = new TextField(id);
        urlBaseTextField.add(new UriValidator("http", "https"));
        return urlBaseTextField;
    }

    public HelpBubble getUrlBaseHelpBubble(String id) {
        return new SchemaHelpBubble(id);
    }

    public Label getServerIdLabel(String id) {
        return new Label(id, "Server ID");
    }

    public Label getServerIdWarningLabel(String id) {
        return new Label(id, "Requires Restart");
    }

    public TextField getServerIdTextField(String id) {
        TextField serverIdTextField = new TextField(id);
        serverIdTextField.add(new ServerIdValidator());
        return serverIdTextField;
    }

    public HelpBubble getServerIdHelpBubble(String id) {
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

    /**
     * {@inheritDoc}
     */
    public String resetPassword(String userName, String remoteAddress, String resetPageUrl) {
        UserGroupService userGroupService = ContextHelper.get().beanForType(UserGroupService.class);
        return userGroupService.resetPassword(userName, remoteAddress, resetPageUrl);
    }

    public WebMarkupContainer getHomeLink(String wicketId) {
        return new LogoLinkContainer(wicketId);
    }

    public boolean isInstantiationAuthorized(Class componentClass) {
        return true;
    }

    public MenuNode getHomeButton(String wicketId) {
        return new MenuNode(wicketId, HomePage.class);
    }

    public Label getUptimeLabel(String wicketId) {
        long uptime = ContextHelper.get().getUptime();
        String uptimeStr = DurationFormatUtils.formatDuration(uptime, "d'd' H'h' m'm' s's'");
        Label uptimeLabel = new Label(wicketId, uptimeStr);
        //Only show uptime for admins
        AuthorizationService authorizationService = ContextHelper.get().getAuthorizationService();
        if (!authorizationService.isAdmin()) {
            uptimeLabel.setVisible(false);
        }
        return uptimeLabel;
    }

    public void addVersionInfo(WebMarkupContainer container, Map<String, String> headersMap) {
        Label currentLabel = new Label("currentLabel", ConstantValues.artifactoryVersion.getString());
        container.add(currentLabel);

        final Label latestLabel = new Label("latestLabel", "");
        latestLabel.setOutputMarkupId(true);
        String latestWikiUrl = VersionHolder.VERSION_UNAVAILABLE.getWikiUrl();
        CentralConfigService centralConfigService = ContextHelper.get().getCentralConfig();
        CentralConfigDescriptor configDescriptor = centralConfigService.getDescriptor();
        if (!configDescriptor.isOfflineMode()) {
            // try to get the latest version from the cache with a non-blocking call
            String latestVersion = getVersionInfoService().getLatestVersion(headersMap, true);
            latestWikiUrl = getVersionInfoService().getLatestWikiUrl(headersMap, true);
            if (VersionInfoService.SERVICE_UNAVAILABLE.equals(latestVersion)) {
                // send ajax refresh in 5 second and update the latest version with the result
                latestLabel.add(new AbstractAjaxTimerBehavior(Duration.seconds(5)) {
                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new NoAjaxIndicatorDecorator();
                    }

                    @Override
                    protected void onTimer(AjaxRequestTarget target) {
                        stop(); // try only once
                        String latestVersion = getVersionInfoService().getLatestVersionFromCache(true);
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
        container.add(latestLabel);

        ExternalLink wikiLink = new ExternalLink("wikiLink", latestWikiUrl);
        container.add(wikiLink);
    }

    public boolean isNewAdminAccountAllowed() {
        return true;
    }

    public boolean isServerIdValid(String serverId) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        return addonsManager.isServerIdValid(serverId);
    }

    public boolean isLogbackConversionRequired(ArtifactoryConfigVersion versionBeforeConversion) {
        return versionBeforeConversion.getComparator().isBefore(ArtifactoryVersion.v210);
    }

    public SaveSearchResultsPanel getSaveSearchResultsPanel(String wicketId, IModel model,
            LimitlessCapableSearcher limitlessCapableSearcher) {
        SaveSearchResultsPanel panel = new SaveSearchResultsPanel(wicketId, model);
        panel.setEnabled(false);
        return panel;
    }

    public MenuNode getBrowserSearchMenuNode() {
        return new DisabledAddonMenuNode("Search Results", SEARCH);
    }

    public String getSearchResultsPageMountPath() {
        return "";
    }

    public Panel getBuildSearchResultsPanel(Addon requestingAddon, Build build) {
        return new BuildSearchResultsPanel(requestingAddon, build);
    }

    public ITab getPropertiesTabPanel(ItemInfo itemInfo) {
        return new DisabledAddonTab(new Model("Properties"), PROPERTIES);
    }

    public ITab getSearchPropertiesTabPanel(FolderInfo folderInfo, List<FileInfo> searchResults) {
        return getPropertiesTabPanel(folderInfo);
    }

    public MenuNode getPropertySearchMenuNode(String nodeTitle) {
        return new DisabledAddonMenuNode(nodeTitle, PROPERTIES);
    }

    public ITab getPropertySearchTabPanel(Page parent, String tabTitle) {
        return new DisabledAddonTab(new Model(tabTitle), PROPERTIES);
    }

    public MenuNode getPropertySetsPage(String nodeTitle) {
        return new DisabledAddonMenuNode(nodeTitle, PROPERTIES);
    }

    public WebMarkupContainer getPropertySetsBorder(String borderId, String dragDropId, final RealRepoDescriptor entity,
            List<PropertySet> propertySets) {
        TitledBorder propertySetsBorder = new TitledBorder(borderId) {
            @Override
            protected org.apache.wicket.Component newToolbar(String id) {
                return new DisabledAddonHelpBubble(id, PROPERTIES);
            }
        };
        propertySetsBorder.add(new DisabledCollapsibleBehavior());
        MarkupContainer dragDropContainer = new WebMarkupContainer(dragDropId);
        propertySetsBorder.add(dragDropContainer);
        return propertySetsBorder;
    }

    public Map<RepoPath, Properties> getProperties(List<RepoPath> repoPaths, String... mandatoryKeys) {
        return Maps.newHashMap();
    }

    public MenuNode getKeyPairMenuNode() {
        return new DisabledAddonMenuNode("Key-Pairs", WEBSTART);
    }

    public WebMarkupContainer getKeyPairContainer(String wicketId, String virtualRepoKey, boolean isCreate) {
        WebMarkupContainer container = new WebMarkupContainer(wicketId);
        DropDownChoice keyPairDropDown = new DropDownChoice("keyPair", Collections.emptyList());
        keyPairDropDown.setEnabled(false);
        keyPairDropDown.add(new DisabledAddonBehavior(WEBSTART));
        container.add(keyPairDropDown);
        container.add(new WebMarkupContainer("keyPairMessage"));
        container.add(new DisabledAddonHelpBubble("keyPair.help", WEBSTART));
        return container;
    }

    public ItemAction getWatchAction(RepoPath itemRepoPath) {
        ItemAction action = new ItemAction("") {
            @Override
            public void onAction(ItemEvent e) {
            }
        };
        action.setEnabled(false);

        return action;
    }

    public ITab getWatchersTab(String tabTitle, RepoPath repoPath) {
        return new DisabledAddonTab(new Model(tabTitle), WATCH);
    }

    public MarkupContainer getWatchingSinceLabel(String labelId, RepoPath itemRepoPath) {
        return new PlaceHolder(labelId);
    }

    public MarkupContainer getDirectlyWatchedPathPanel(String panelId, RepoPath itemRepoPath) {
        return new PlaceHolder(panelId);
    }

    public MenuNode getHttpSsoMenuNode(String nodeName) {
        return new DisabledAddonMenuNode(nodeName, Addon.SSO);
    }

    public FieldSetPanel getExportResultPanel(String panelId, ActionableItem item) {
        return new ExportResultsPanel(panelId, item);
    }

    public CustomizingPanel getCustomizingPanel(String panelId) {
        return new CustomizingPanel(panelId);
    }


    public WebMarkupContainer getAddonsInfoPanel(String panelId) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        List<String> installedAddonNames = addonsManager.getInstalledAddonNames();
        List<String> enabledAddonNames = addonsManager.getEnabledAddonNames();
        return new AddonsInfoPanel(panelId, installedAddonNames, enabledAddonNames);
    }

    private VersionInfoService getVersionInfoService() {
        return ContextHelper.get().beanForType(VersionInfoService.class);
    }

    private String buildLatestversionString(String latestVersion) {
        return "(latest release is " + latestVersion + ")";
    }

    private CentralConfigService getCentralConfig() {
        ArtifactoryContext context = ContextHelper.get();
        if (context != null) {
            return context.getCentralConfig();
        }
        return ArtifactoryApplication.get().getCentralConfig();
    }

    public MenuNode getBuildBrowserMenuNode(String displayName) {
        return new DisabledAddonMenuNode(displayName, BUILD);
    }

    public ItemAction getGoToBuildAction(String buildName, long buildNumber) {
        ItemAction action = new ItemAction("") {
            @Override
            public void onAction(ItemEvent e) {
            }
        };
        action.setEnabled(false);

        return action;
    }

    public Panel getLdapGroupPanel(String id, LdapSetting ldapGroupSetting) {
        return new EmptyPanel(id);
    }

    public SortableTable getBuildTabArtifacsTable(String tableId, List<IColumn> columns, ModalHandler textContentViewer,
            String repoItemSha1, String repoItemMd5) {
        return getDisabledTable(tableId, columns);
    }

    public SortableTable getBuildTabDependenciesTable(String tableId, List<IColumn> columns,
            ModalHandler textContentViewer,
            String repoItemSha1, String repoItemMd5) {
        return getDisabledTable(tableId, columns);
    }

    public SortableTable getModuleArtifactsTable(String tableId, List<IColumn> columns, List<Artifact> artifacts) {
        return getDisabledTable(tableId, columns);
    }

    public SortableTable getModuleDependenciesTable(String tableId, List<IColumn> columns,
            List<Dependency> dependencies) {
        return getDisabledTable(tableId, columns);
    }

    /**
     * Returns a disabled sortable table
     *
     * @param tableId ID to assign to the table
     * @param columns Columns to dispaly in the table
     * @return Disabled sortable table
     */
    private SortableTable getDisabledTable(String tableId, List<IColumn> columns) {
        SortableDataProvider dataProvider = new SortableDataProvider() {

            public Iterator iterator(int first, int count) {
                return Iterators.emptyIterator();
            }

            public int size() {
                return 0;
            }

            public IModel model(Object object) {
                return null;
            }
        };
        SortableTable table = new SortableTable(tableId, columns, dataProvider, 1);
        table.setEnabled(false);
        return table;
    }

    public ITab getBuildsTab(final RepoAwareActionableItem item) {
        return new BaseTab("Builds") {
            @Override
            public Panel getPanel(String panelId) {
                return new DisabledBuildsTabPanel(panelId, item);
            }

            @Override
            public void onNewTabItem(Loop.LoopItem item) {
                super.onNewTabItem(item);
                item.add(new AddonNeededBehavior(Addon.BUILD));
            }
        };
    }

    public ITab getModuleInfoTab(String buildName, long buildNumber, final Module module) {
        return new BaseTab("Published Modules") {
            @Override
            public Panel getPanel(String panelId) {
                return new DisabledModuleInfoTabPanel(panelId);
            }

            @Override
            public void onNewTabItem(Loop.LoopItem item) {
                super.onNewTabItem(item);
                item.add(new AddonNeededBehavior(Addon.BUILD));
            }
        };
    }

    public String getDeleteItemWarningMessage(ItemInfo item, String defaultMessage) {
        return defaultMessage;
    }

    public String getDeleteVersionsWarningMessage(List<RepoPath> versionPaths, String defaultMessage) {
        return defaultMessage;
    }

    public ITab getLdapGroupTab(LdapSetting ldapSetting) {
        return new DisabledAddonTab(new Model("LDAP Groups"), Addon.LDAP);
    }

    public Set<FileInfo> getArtifactFileInfo(Build build) {
        return Sets.newHashSet();
    }

    public Set<FileInfo> getDependencyFileInfo(Build build) {
        return Sets.newHashSet();
    }

    public List<ModuleArtifactActionableItem> getModuleArtifactActionableItems(String buildName, long buildNumber,
            List<Artifact> artifacts) {
        return Lists.newArrayList();
    }

    public List<ModuleDependencyActionableItem> getModuleDependencyActionableItem(String buildName, long buildNumber,
            List<Dependency> dependencies) {
        return Lists.newArrayList();
    }
}