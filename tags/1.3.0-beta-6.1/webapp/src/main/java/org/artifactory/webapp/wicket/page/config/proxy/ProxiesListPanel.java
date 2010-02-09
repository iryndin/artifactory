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
package org.artifactory.webapp.wicket.page.config.proxy;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import static org.artifactory.webapp.wicket.common.component.CreateUpdateAction.CREATE;
import static org.artifactory.webapp.wicket.common.component.CreateUpdateAction.UPDATE;
import org.artifactory.webapp.wicket.common.component.modal.panel.BaseModalPanel;
import org.artifactory.webapp.wicket.common.component.panel.list.ListPanel;

import java.util.List;

/**
 * @author Yossi Shaul
 */
public class ProxiesListPanel extends ListPanel<ProxyDescriptor> {
    @SpringBean
    private CentralConfigService centralConfigService;

    public ProxiesListPanel(String id) {
        super(id);
    }

    @Override
    public String getTitle() {
        return "Proxies";
    }

    @Override
    protected BaseModalPanel newCreateItemPanel() {
        return new ProxyCreateUpdatePanel(CREATE, new ProxyDescriptor(), this);
    }

    @Override
    protected BaseModalPanel newUpdateItemPanel(ProxyDescriptor itemObject) {
        return new ProxyCreateUpdatePanel(UPDATE, itemObject, this);
    }

    @Override
    protected String getDeleteConfirmationText(ProxyDescriptor itemObject) {
        String name = itemObject.getKey();
        return "Are you sure you wish to delete the proxy " + name + "?";
    }

    @Override
    protected void deleteItem(ProxyDescriptor itemObject, AjaxRequestTarget target) {
        MutableCentralConfigDescriptor centralConfig =
                centralConfigService.getDescriptorForEditing();
        centralConfig.removeProxy(itemObject.getKey());
        centralConfigService.saveEditedDescriptorAndReload();
    }

    @Override
    protected List<ProxyDescriptor> getList() {
        return centralConfigService.getDescriptorForEditing().getProxies();
    }

    @Override
    protected void addColumns(List<IColumn> columns) {
        columns.add(new PropertyColumn(new Model("Proxy Key"), "key", "key"));
        columns.add(new PropertyColumn(new Model("Host"), "host", "host"));
        columns.add(new PropertyColumn(new Model("Port"), "port", "port"));
    }
}