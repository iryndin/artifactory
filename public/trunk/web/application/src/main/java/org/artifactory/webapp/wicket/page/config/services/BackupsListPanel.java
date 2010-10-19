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

package org.artifactory.webapp.wicket.page.config.services;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.panel.list.ModalListPanel;
import org.artifactory.common.wicket.component.table.columns.BooleanColumn;
import org.artifactory.common.wicket.component.table.columns.TitlePropertyColumn;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;

import java.util.List;

/**
 * @author Yossi Shaul
 */
public class BackupsListPanel extends ModalListPanel<BackupDescriptor> {

    @SpringBean
    private CentralConfigService centralConfigService;

    private final MutableCentralConfigDescriptor mutableCentralConfig;

    public BackupsListPanel(String id) {
        super(id);
        mutableCentralConfig = centralConfigService.getMutableDescriptor();
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    protected List<BackupDescriptor> getList() {
        return mutableCentralConfig.getBackups();
    }

    @Override
    protected void addColumns(List<IColumn> columns) {
        columns.add(new TitlePropertyColumn("Backup Key", "key", "key"));
        columns.add(new TitlePropertyColumn("Cron Expression", "cronExp"));
        columns.add(new BooleanColumn("Enabled", "enabled", "enabled"));
    }

    @Override
    protected BaseModalPanel newCreateItemPanel() {
        return new BackupCreateUpdatePanel(CreateUpdateAction.CREATE, new BackupDescriptor(), this);
    }

    @Override
    protected BaseModalPanel newUpdateItemPanel(BackupDescriptor backup) {
        return new BackupCreateUpdatePanel(CreateUpdateAction.UPDATE, backup, this);
    }

    @Override
    protected String getDeleteConfirmationText(BackupDescriptor backup) {
        String key = backup.getKey();
        return "Are you sure you wish to delete the backup '" + key + "'?";
    }

    @Override
    protected void deleteItem(BackupDescriptor backup, AjaxRequestTarget target) {
        mutableCentralConfig.removeBackup(backup.getKey());
        centralConfigService.saveEditedDescriptorAndReload(mutableCentralConfig);
    }

    MutableCentralConfigDescriptor getMutableDescriptor() {
        return mutableCentralConfig;
    }
}