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
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.webapp.wicket.component.panel.titled.TitledPanel;
import static org.artifactory.webapp.wicket.utils.WebUtils.getWicketServletContextUrl;

import java.util.List;

/**
 * @author Yoav Landman
 */
public class VirtualRepoListPanel extends TitledPanel {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(VirtualRepoListPanel.class);

    @SpringBean
    private RepositoryService repositoryService;

    public VirtualRepoListPanel(String string) {
        super(string);
        final String hrefPrefix = getWicketServletContextUrl();
        List<VirtualRepoDescriptor> repoList = repositoryService.getVirtualRepoDescriptors();
        add(new ListView("repos", repoList) {
            @Override
            protected void populateItem(ListItem item) {
                VirtualRepoDescriptor repo = (VirtualRepoDescriptor) item.getModelObject();
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