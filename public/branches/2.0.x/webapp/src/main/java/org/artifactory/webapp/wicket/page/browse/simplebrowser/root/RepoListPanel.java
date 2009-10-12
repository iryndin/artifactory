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
package org.artifactory.webapp.wicket.page.browse.simplebrowser.root;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RepoBaseDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledPanel;
import org.artifactory.webapp.wicket.utils.CssClass;
import static org.artifactory.webapp.wicket.utils.WebUtils.getWicketServletContextUrl;

import java.util.List;

/**
 * @author Yoav Landman
 */
public class RepoListPanel extends TitledPanel {
    private static final long serialVersionUID = 1L;


    public RepoListPanel(String id, List<? extends RepoBaseDescriptor> repoDescriptorList) {
        super(id);
        final String hrefPrefix = getWicketServletContextUrl();
        add(new ListView("repos", repoDescriptorList) {
            @Override
            protected void populateItem(ListItem item) {
                RepoBaseDescriptor repo = (RepoBaseDescriptor) item.getModelObject();
                String key = repo.getKey();
                String href = hrefPrefix + "/" + key + "/";
                ExternalLink link = new ExternalLink("link", href, key);

                String cssClass = getCssClass(repo);
                link.add(new org.artifactory.webapp.wicket.common.behavior.CssClass(cssClass));
                item.add(link);
            }
        });
    }

    @Override
    public String getTitle() {
        return getString(getId(), null);
    }

    private String getCssClass(RepoBaseDescriptor repo) {
        if (repo instanceof VirtualRepoDescriptor) {
            return CssClass.repositoryVirtual.cssClass();
        }

        if (repo instanceof RealRepoDescriptor && ((RealRepoDescriptor) repo).isCache()) {
            return CssClass.repositoryCache.cssClass();
        }

        return CssClass.repository.cssClass();
    }

}