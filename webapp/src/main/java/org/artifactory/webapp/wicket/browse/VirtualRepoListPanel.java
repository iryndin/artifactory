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
package org.artifactory.webapp.wicket.browse;

import org.apache.log4j.Logger;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.artifactory.config.CentralConfig;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.webapp.wicket.components.panel.TitlePanel;
import org.artifactory.webapp.wicket.utils.ServletUtils;

import java.util.List;

/**
 * @author Yoav Landman
 */
public class VirtualRepoListPanel extends TitlePanel {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(BrowseRepoPanel.class);

    public VirtualRepoListPanel(String string) {
        super(string);
        final String hrefPrefix = ServletUtils.getServletContextUrl();
        CentralConfig cc = CentralConfig.get();
        List<VirtualRepo> repoList = cc.getVirtualRepositories();
        add(new ListView("repos", repoList) {
            protected void populateItem(ListItem item) {
                VirtualRepo repo = (VirtualRepo) item.getModelObject();
                String key = repo.getKey();
                String href = hrefPrefix + "/" + key + "/";
                item.add(new ExternalLink("link", href, key));
                String icon = "repository-virtual.png";
                item.add(new Image("icon", new ResourceReference("/images/" + icon)));
            }
        });
    }

    @Override
    public String getTitle() {
        return "Virtual Repositories";
    }
}