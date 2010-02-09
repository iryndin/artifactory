/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.home.addon;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonInfo;
import org.artifactory.addon.AddonsManager;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.webapp.wicket.page.config.general.GeneralConfigPage;

import java.util.Collection;
import java.util.List;

/**
 * Displays the table of the installed addons
 *
 * @author Noam Y. Tenne
 */
public class AddonsInfoPanel extends TitledPanel {

    @SpringBean
    private AddonsManager addonsManager;

    @SpringBean
    private AuthorizationService authorizationService;

    @SpringBean
    private CentralConfigService centralConfigService;

    /**
     * Main constructor
     *
     * @param id                  ID to assign to panel
     * @param installedAddonNames Name list of installed addons
     * @param enabledAddonNames   Name list of enabled addons
     */
    public AddonsInfoPanel(String id, final List<String> installedAddonNames,
            final Collection<String> enabledAddonNames) {
        super(id);
        add(new CssClass("addons-table"));

        String currentServerId = centralConfigService.getDescriptor().getAddons().getServerId();
        final boolean currentServerIdValid = addonsManager.isServerIdValid(currentServerId);
        final boolean serverIdChanged = addonsManager.isServerIdChanged();

        MarkupContainer addonTable = new WebMarkupContainer("addonTable");
        boolean noAddons = installedAddonNames.isEmpty();
        boolean admin = authorizationService.isAdmin();

        addonTable.setVisible(!noAddons);
        addonTable.setOutputMarkupId(true);

        Component listView = new ListView("addonItem", installedAddonNames) {
            @Override
            protected void populateItem(ListItem item) {
                final String addonName = item.getModelObjectAsString();
                AddonInfo addonInfo = addonsManager.getAddonInfoByName(addonName);

                item.add(new Label("name", addonInfo.getAddonDisplayName()));
                item.add(new Label("image", "").add(new CssClass("addon-" + addonInfo.getAddonName())));

                if (enabledAddonNames.contains(addonName)) {
                    item.add(new Label("status", "Enabled").add(new CssClass("addon-enabled")));
                } else {
                    item.add(new Label("status", "Disabled").add(new CssClass("addon-disabled")));
                }

                if (item.getIndex() % 2 == 1) {
                    item.add(new CssClass("even"));
                }
            }
        };
        addonTable.add(listView);
        add(addonTable);

        CharSequence generalConfigPage = WicketUtils.mountPathForPage(GeneralConfigPage.class);

        boolean noEnabledAddons = enabledAddonNames.isEmpty();
        add(new Label("addonsDisabled", "All add-ons are disabled")
                .setVisible(!serverIdChanged && currentServerIdValid && !noAddons && noEnabledAddons));

        add(new Label("requiresRestartValid", "Please restart Artifactory for the new server ID to take effect.")
                .setVisible(admin && currentServerIdValid && serverIdChanged && !noAddons));

        Label removedServerIdLabel = new Label("requiresRestartInvalid",
                String.format("Your <a href='%s#serverId'>Server ID</a> is invalid. Please enter a valid one.",
                        generalConfigPage));
        removedServerIdLabel.setVisible(admin && !currentServerIdValid && serverIdChanged && !noAddons);
        removedServerIdLabel.setEscapeModelStrings(false);
        add(removedServerIdLabel);

        add(new Label("noAddons", "No add-ons currently installed.").setVisible(noAddons));
        Label noServerIdLabel = new Label("noServerId", String.format("Add-ons are currently disabled. To enable " +
                "add-ons you need to enter your <a href='%s#serverId'>Server ID</a> first.", generalConfigPage));
        noServerIdLabel.setVisible(admin && !currentServerIdValid && !serverIdChanged && !noAddons && noEnabledAddons);
        noServerIdLabel.setEscapeModelStrings(false);

        add(noServerIdLabel);
    }

    @Override
    public String getTitle() {
        return "Installed Add-ons";
    }
}