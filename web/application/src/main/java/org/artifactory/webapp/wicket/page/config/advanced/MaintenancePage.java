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

package org.artifactory.webapp.wicket.page.config.advanced;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.storage.StorageService;
import org.artifactory.common.wicket.ajax.ConfirmationAjaxCallDecorator;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxLink;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;
import org.slf4j.Logger;

/**
 * Displays the different maintenance controls to the user
 *
 * @author Noam Tenne
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class MaintenancePage extends AuthenticatedPage {

    private static final Logger log = LoggerFactory.getLogger(MaintenancePage.class);

    @SpringBean
    private StorageService storageService;
    private TitledBorder border = new TitledBorder("storage");

    public MaintenancePage() {
        addStorageMaintenance();
        addGarbageCollectorMaintenance();
    }

    /**
     * Add the storage maintenance control to the page
     */
    private void addStorageMaintenance() {
        add(border);
        // add the compress link
        TitledAjaxLink compressLink = new TitledAjaxLink("compress", "Compress the Internal Database") {
            public void onClick(AjaxRequestTarget target) {
                MultiStatusHolder statusHolder = new MultiStatusHolder();
                try {
                    storageService.compress(statusHolder);
                } catch (Exception e) {
                    statusHolder.setError(e.getMessage(), log);
                } finally {
                    if (statusHolder.isError()) {
                        error("Failed to compress database: " + statusHolder.getLastError().getMessage());
                    } else {
                        info("Database successfully compressed.");
                    }
                }
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new ConfirmationAjaxCallDecorator(super.getAjaxCallDecorator(),
                        "Are you sure you want to compress the internal database? (Overall performance will degrade " +
                                "until compression completes).");
            }
        };
        boolean isDerbyUsed = storageService.isDerbyUsed();
        compressLink.setVisible(isDerbyUsed);
        border.add(compressLink);
        HelpBubble compressHelpBubble = new HelpBubble("compressHelp", new ResourceModel("compressHelp"));
        compressHelpBubble.setVisible(isDerbyUsed);
        border.add(compressHelpBubble);
    }

    private void addGarbageCollectorMaintenance() {
        add(border);
        TitledAjaxLink compressLink = new TitledAjaxLink("collect", "Run Garbage Collector Now") {
            public void onClick(AjaxRequestTarget target) {
                MultiStatusHolder statusHolder = new MultiStatusHolder();
                storageService.manualGarbageCollect(statusHolder);
                if (statusHolder.isError()) {
                    error("Could not run the garbage collector: " + statusHolder.getLastError().getMessage() + ".");
                } else {
                    info("Garbage collector was successfully scheduled to run in the background.");
                }
            }
        };
        border.add(compressLink);
        HelpBubble gcHelpBubble = new HelpBubble("gcHelp", new ResourceModel("garbageHelp"));
        border.add(gcHelpBubble);

    }

    @Override
    public String getPageName() {
        return "Maintenance";
    }
}
