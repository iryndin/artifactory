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

package org.artifactory.webapp.wicket.page.config.repos.remote;

import com.google.common.collect.Lists;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.artifactory.addon.wicket.PropertiesWebAddon;
import org.artifactory.addon.wicket.ReplicationWebAddon;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.descriptor.replication.RemoteReplicationDescriptor;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.util.HttpClientConfigurator;
import org.artifactory.webapp.wicket.page.config.repos.CachingDescriptorHelper;
import org.artifactory.webapp.wicket.page.config.repos.RepoConfigCreateUpdatePanel;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Remote repository configuration panel.
 *
 * @author Yossi Shaul
 */
public class HttpRepoPanel extends RepoConfigCreateUpdatePanel<HttpRepoDescriptor> {

    private final CreateUpdateAction action;
    private RemoteReplicationDescriptor replicationDescriptor;

    public HttpRepoPanel(CreateUpdateAction action, HttpRepoDescriptor repoDescriptor,
            CachingDescriptorHelper cachingDescriptorHelper) {
        super(action, repoDescriptor, cachingDescriptorHelper);
        setWidth(640);
        this.action = action;
    }

    @Override
    protected List<ITab> getConfigurationTabs() {
        List<ITab> tabs = Lists.newArrayList();

        tabs.add(new AbstractTab(Model.<String>of("Basic Settings")) {
            @Override
            public Panel getPanel(String panelId) {
                return new HttpRepoBasicPanel(panelId, entity);
            }
        });

        tabs.add(new AbstractTab(Model.<String>of("Advanced Settings")) {
            @Override
            public Panel getPanel(String panelId) {
                return new HttpRepoAdvancedPanel(panelId, action, entity,
                        cachingDescriptorHelper.getModelMutableDescriptor());
            }
        });

        PropertiesWebAddon propertiesWebAddon = addons.addonByType(PropertiesWebAddon.class);
        List<PropertySet> propertySets = getCachingDescriptorHelper().getModelMutableDescriptor().getPropertySets();
        tabs.add(propertiesWebAddon.getRepoConfigPropertySetsTab("Property Sets", entity, propertySets));

        replicationDescriptor = cachingDescriptorHelper.getModelMutableDescriptor().getRemoteReplication(
                entity.getKey());
        if (replicationDescriptor == null) {
            replicationDescriptor = new RemoteReplicationDescriptor();
            replicationDescriptor.setRepoKey(entity.getKey());
        }
        ReplicationWebAddon replicationWebAddon = addons.addonByType(ReplicationWebAddon.class);
        tabs.add(replicationWebAddon.getHttpRepoReplicationPanel("Replication", entity, replicationDescriptor));

        return tabs;
    }

    @Override
    public void addAndSaveDescriptor(HttpRepoDescriptor repoDescriptor) {
        CachingDescriptorHelper helper = getCachingDescriptorHelper();
        MutableCentralConfigDescriptor mccd = helper.getModelMutableDescriptor();
        repoDescriptor.setKey(key);
        mccd.addRemoteRepository(repoDescriptor);
        if (replicationDescriptor.isEnabled()) {
            if (StringUtils.isBlank(replicationDescriptor.getRepoKey())) {
                replicationDescriptor.setRepoKey(key);
            }
            mccd.addRemoteReplication(replicationDescriptor);
        }
        helper.syncAndSaveRemoteRepositories();
    }

    @Override
    public void saveEditDescriptor(HttpRepoDescriptor repoDescriptor) {
        CachingDescriptorHelper helper = getCachingDescriptorHelper();
        MutableCentralConfigDescriptor mccd = helper.getModelMutableDescriptor();
        if (replicationDescriptor.isEnabled() && !mccd.isRemoteReplicationExists(replicationDescriptor)) {
            if (StringUtils.isBlank(replicationDescriptor.getRepoKey())) {
                replicationDescriptor.setRepoKey(key);
            }
            mccd.addRemoteReplication(replicationDescriptor);
        }
        helper.syncAndSaveRemoteRepositories();
    }

    @Override
    protected boolean validate(HttpRepoDescriptor repoDescriptor) {
        boolean urlValid = StringUtils.isNotEmpty(repoDescriptor.getUrl());
        if (!urlValid) {
            error("Field 'Url' is required.");
        }
        return urlValid;
    }

    @Override
    protected TitledAjaxSubmitLink createTestButton() {
        return new TitledAjaxSubmitLink("test", "Test", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                HttpRepoDescriptor repo = getRepoDescriptor();
                if (!validate(repo)) {
                    AjaxUtils.refreshFeedback();
                    return;
                }
                HttpClient client = new HttpClientConfigurator(true)
                        .hostFromUrl(repo.getUrl())
                        .defaultMaxConnectionsPerHost(50)
                        .maxTotalConnections(50)
                        .connectionTimeout(repo.getSocketTimeoutMillis())
                        .soTimeout(repo.getSocketTimeoutMillis())
                        .staleCheckingEnabled(true)
                        .retry(1, false)
                        .localAddress(repo.getLocalAddress())
                        .proxy(repo.getProxy())
                        .authentication(repo.getUsername(), repo.getPassword())
                        .getClient();
                HeadMethod head = new HeadMethod(repo.getUrl());
                try {
                    int status = client.executeMethod(head);
                    if (status != HttpStatus.SC_OK) {
                        String reason = head.getStatusText();
                        error("Connection failed: Error " + status + ": " + reason);
                    } else {
                        info("Successfully connected to server");
                    }
                } catch (UnknownHostException e) {
                    error("Unknown host: " + e.getMessage());
                } catch (ConnectException e) {
                    error(e.getMessage());
                } catch (IOException e) {
                    error("Connection failed with exception: " + e.getMessage());
                } finally {
                    head.releaseConnection();
                }
                AjaxUtils.refreshFeedback(target);
            }
        };
    }
}
