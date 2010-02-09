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
import org.apache.wicket.protocol.http.servlet.AbortWithWebErrorCodeException;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.repo.DirectoryItem;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.utils.MimeTypes;
import org.artifactory.webapp.wicket.component.panel.titled.TitledPanel;
import org.artifactory.webapp.wicket.utils.WebUtils;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author Yoav Landman
 */
public class LocalRepoBrowserPanel extends TitledPanel {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(LocalRepoBrowserPanel.class);

    @SpringBean
    private RepositoryService repoService;

    private final RepoPath repoPath;

    public LocalRepoBrowserPanel(String id, final RepoPath repoPath) {
        super(id);
        this.repoPath = repoPath;
        List<DirectoryItem> dirItems = repoService.getDirectoryItems(repoPath, true);
        if (dirItems == null) {
            throw new AbortWithWebErrorCodeException(HttpServletResponse.SC_NOT_FOUND);
        }
        final String hrefPrefix = WebUtils.getWicketServletContextUrl();
        add(new ListView("items", dirItems) {
            @Override
            protected void populateItem(ListItem item) {
                DirectoryItem directoryItem = (DirectoryItem) item.getModelObject();
                String directoryPath = directoryItem.getPath();
                if ("/".equals(directoryPath)) {
                    //Do not repeat / twice for root repo link
                    directoryPath = "";
                } else {
                    if (directoryItem.isFolder() && directoryPath.length() > 0) {
                        directoryPath += "/";
                    }
                }
                String href = hrefPrefix + "/" + repoPath.getRepoKey() + "/" + directoryPath;
                item.add(new ExternalLink("link", href, directoryItem.getName()));
                item.add(new Image("icon",
                        new ResourceReference("/images/" + getIcon(directoryItem))));
            }
        });
    }

    public String getIcon(DirectoryItem dirItem) {
        ItemInfo itemInfo = dirItem.getItemInfo();
        String path = itemInfo.getRelPath();
        if (itemInfo.isFolder()) {
            return "folder.png";
        } else {
            if (MimeTypes.isJarVariant(path)) {
                return "jar.png";
            } else if (dirItem.getName().equals(DirectoryItem.UP)) {
                return "parent.png";
            } else if (path.endsWith(".pom")) {
                return "pom.png";
            } else if (path.endsWith(".xml")) {
                return "xml.png";
            } else {
                return "doc.png";
            }
        }
    }

    @Override
    public String getTitle() {
        return repoPath.toString();
    }

}