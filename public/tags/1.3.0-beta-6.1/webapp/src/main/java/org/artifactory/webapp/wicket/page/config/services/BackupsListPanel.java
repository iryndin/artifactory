/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.wicket.page.config.services;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.modal.panel.BaseModalPanel;
import org.artifactory.webapp.wicket.common.component.panel.list.ListPanel;

import java.util.List;

/**
 * @author Yossi Shaul
 */
public class BackupsListPanel extends ListPanel<BackupDescriptor> {

    @SpringBean
    private CentralConfigService centralConfigService;

    public BackupsListPanel(String id) {
        super(id);
    }

    @Override
    public String getTitle() {
        return "Backups";
    }

    @Override
    protected List<BackupDescriptor> getList() {
        return getDescriptorForEditing().getBackups();
    }

    @Override
    protected void addColumns(List<IColumn> columns) {
        columns.add(new PropertyColumn(new Model("Backup Key"), "key"));
    }

    @Override
    protected BaseModalPanel newCreateItemPanel() {
        return new BackupCreateUpdatePanel(CreateUpdateAction.CREATE, new BackupDescriptor());
    }

    @Override
    protected BaseModalPanel newUpdateItemPanel(BackupDescriptor backup) {
        return new BackupCreateUpdatePanel(CreateUpdateAction.UPDATE, backup);
    }

    @Override
    protected String getDeleteConfirmationText(BackupDescriptor backup) {
        String key = backup.getKey();
        return "Are you sure you wish to delete the backup " + key + "?";
    }

    @Override
    protected void deleteItem(BackupDescriptor backup, AjaxRequestTarget target) {
        MutableCentralConfigDescriptor centralConfig = getDescriptorForEditing();
        centralConfig.removeBackup(backup.getKey());
        centralConfigService.saveEditedDescriptorAndReload();
        ((ServicesConfigPage) getPage()).refresh(target);
    }

    private MutableCentralConfigDescriptor getDescriptorForEditing() {
        return centralConfigService.getDescriptorForEditing();
    }

}