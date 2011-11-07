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

package org.artifactory.webapp.wicket.page.config.advanced;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.storage.StorageService;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.wicket.ajax.ConfirmationAjaxCallDecorator;
import org.artifactory.common.wicket.component.CancelLink;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxLink;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.gc.GcConfigDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.page.config.services.cron.CronNextDatePanel;
import org.artifactory.webapp.wicket.util.validation.CronExpValidator;
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
    private CentralConfigService centralConfigService;

    @SpringBean
    private StorageService storageService;

    public MaintenancePage() {
        setOutputMarkupId(true);
        addStorageMaintenance();
        addGarbageCollectorMaintenance();
    }

    /**
     * Add the storage maintenance control to the page
     */
    private void addStorageMaintenance() {
        TitledBorder border = new TitledBorder("storage");
        add(border);

        // add the compress link
        TitledAjaxLink compressLink = new TitledAjaxLink("compress", "Compress the Internal Database") {
            @Override
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
        border.add(compressLink);
        HelpBubble compressHelpBubble = new HelpBubble("compressHelp", new ResourceModel("compressHelp"));
        border.add(compressHelpBubble);

        // add the prune link
        TitledAjaxLink pruneLink = new TitledAjaxLink("prune", "Prune Unreferenced Data") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                MultiStatusHolder statusHolder = new MultiStatusHolder();
                storageService.pruneUnreferencedFileInDataStore(statusHolder);
                if (statusHolder.isError()) {
                    error("Pruning unreferenced data completed with an error:\n" +
                            statusHolder.getLastError().getMessage() + ".");
                } else {
                    info("Pruning unreferenced data completed successfully!\n" + statusHolder.getStatusMsg());
                }
            }
        };
        border.add(pruneLink);
        HelpBubble pruneHelpBubble = new HelpBubble("pruneHelp", new ResourceModel("pruneHelp"));
        border.add(pruneHelpBubble);

        // Compress only valid for Derby DB
        boolean isDerbyUsed = storageService.isDerbyUsed();
        compressLink.setVisible(isDerbyUsed);
        compressHelpBubble.setVisible(isDerbyUsed);
    }

    private void addGarbageCollectorMaintenance() {
        final Border gcBorder = new TitledBorder("gcBorder");
        add(gcBorder);
        Form<GcConfigDescriptor> gcForm =
                new Form<GcConfigDescriptor>("gcForm", new CompoundPropertyModel<GcConfigDescriptor>(
                        centralConfigService.getMutableDescriptor().getGcConfig()));
        gcBorder.add(gcForm);
        TextField<String> cronExpTextField = new TextField<String>("cronExp");
        cronExpTextField.setRequired(true);
        cronExpTextField.add(CronExpValidator.getInstance());
        gcForm.add(cronExpTextField);
        gcForm.add(new SchemaHelpBubble("cronExp.help"));
        gcForm.add(new CronNextDatePanel("cronNextDatePanel", cronExpTextField));
        add(new TitledAjaxSubmitLink("saveGcButton", "Save", gcForm) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                MutableCentralConfigDescriptor mutableDescriptor = centralConfigService.getMutableDescriptor();
                mutableDescriptor.setGcConfig(((GcConfigDescriptor) form.getModelObject()));
                centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
                info("Garbage collection settings were successfully saved.");
                AjaxUtils.refreshFeedback();
            }
        });
        add(new CancelLink("cancel", gcForm) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(MaintenancePage.class);
            }
        });

        String buttonText =
                ConstantValues.gcUseV1.getBoolean() ? "Run Storage GC and Fix Consistency" :
                        "Run Storage Garbage Collection";
        TitledAjaxLink collectLink = new TitledAjaxLink("collect", buttonText) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                MultiStatusHolder statusHolder = new MultiStatusHolder();
                storageService.callManualGarbageCollect(statusHolder);
                if (statusHolder.isError()) {
                    error("Could not run the garbage collector: " + statusHolder.getLastError().getMessage() + ".");
                } else {
                    info("Garbage collector was successfully scheduled to run in the background.");
                }
            }
        };
        gcForm.add(collectLink);
        HelpBubble gcHelpBubble = new HelpBubble("gcHelp", new ResourceModel("garbageHelp"));
        gcForm.add(gcHelpBubble);
    }

    @Override
    public String getPageName() {
        return "Maintenance";
    }
}