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
package org.artifactory.webapp.wicket.page.config.security;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.modal.panel.BaseModalPanel;
import org.artifactory.webapp.wicket.common.component.panel.list.ListPanel;

import java.util.Collections;
import java.util.List;

/**
 * @author Yossi Shaul
 */
public class LdapsListPanel extends ListPanel<LdapSetting> {

    @SpringBean
    private CentralConfigService centralConfigService;

    public LdapsListPanel(String id) {
        super(id);
        setOutputMarkupId(true);
    }

    public String getTitle() {
        return "";
    }

    protected List<LdapSetting> getList() {
        MutableCentralConfigDescriptor centralConfig =
                centralConfigService.getDescriptorForEditing();
        SecurityDescriptor security = centralConfig.getSecurity();
        List<LdapSetting> ldaps = security.getLdapSettings();
        if (ldaps != null) {
            return ldaps;
        }
        return Collections.emptyList();
    }

    protected void addColumns(List<IColumn> columns) {
        columns.add(new PropertyColumn(new Model("Ldap Key"), "key", "key"));
    }

    protected BaseModalPanel newCreateItemPanel() {
        return new LdapCreateUpdatePanel(CreateUpdateAction.CREATE, new LdapSetting(), this);
    }

    protected BaseModalPanel newUpdateItemPanel(LdapSetting ldapSetting) {
        return new LdapCreateUpdatePanel(CreateUpdateAction.UPDATE, ldapSetting, this);
    }

    protected String getDeleteConfirmationText(LdapSetting ldapSetting) {
        return "Are you sure you wish to delete the ldap " + ldapSetting.getKey() + "?";
    }

    protected void deleteItem(LdapSetting ldapSetting, AjaxRequestTarget target) {
        MutableCentralConfigDescriptor centralConfig =
                centralConfigService.getDescriptorForEditing();
        SecurityDescriptor securityDescriptor = centralConfig.getSecurity();
        securityDescriptor.removeLdap(ldapSetting.getKey());
        centralConfigService.saveEditedDescriptorAndReload();
    }

}