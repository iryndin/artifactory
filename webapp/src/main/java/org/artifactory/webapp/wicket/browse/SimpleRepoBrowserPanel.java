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
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.servlet.AbortWithWebErrorCodeException;
import org.artifactory.config.CentralConfig;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrFolder;
import org.artifactory.jcr.JcrFsItem;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.security.RepoPath;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.webapp.servlet.RepoFilter;
import org.artifactory.webapp.wicket.panel.TitlePanel;
import org.springframework.util.StringUtils;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Yoav Landman
 */
public class SimpleRepoBrowserPanel extends TitlePanel {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(BrowseRepoPanel.class);

    private RepoPath repoPath;

    public SimpleRepoBrowserPanel(String string) {
        super(string);
        //Retrieve the repository path from thes request
        WebRequestCycle webRequestCycle = (WebRequestCycle) getRequestCycle();
        WebRequest request = webRequestCycle.getWebRequest();
        HttpServletRequest httpRequest = request.getHttpServletRequest();
        HttpSession httpSession = httpRequest.getSession();
        repoPath = (RepoPath) httpSession.getAttribute(
                RepoFilter.ATTR_ARTIFACTORY_REPOSITORY_PATH);
        httpSession.removeAttribute(RepoFilter.ATTR_ARTIFACTORY_REPOSITORY_PATH);
        if (repoPath == null) {
            //Happens on refresh after login redirection - return a 404
            throw new AbortWithWebErrorCodeException(HttpServletResponse.SC_NOT_FOUND);
        }
        final String repoKey = repoPath.getRepoKey();
        CentralConfig cc = CentralConfig.get();
        VirtualRepo globalVirtualRepo = cc.getGlobalVirtualRepo();
        VirtualRepo virtualRepo = null;
        final LocalRepo repo = globalVirtualRepo.localOrCachedRepositoryByKey(repoKey);
        final String hrefPrefix = httpRequest.getScheme() + "://" +
                httpRequest.getServerName() + ":" +
                httpRequest.getServerPort() +
                httpRequest.getContextPath();
        if (repo == null) {
            //Try to get a virtual repo
            virtualRepo = cc.virtualRepositoryByKey(repoKey);
            if (virtualRepo == null) {
                //Return a 404
                throw new AbortWithWebErrorCodeException(HttpServletResponse.SC_NOT_FOUND);
            }
        }
        if (virtualRepo != null) {
            //We are viewing a virtual repository
            List<LocalRepo> repos = virtualRepo.getLocalAndCachedRepositories();
            add(new ListView("items", repos) {
                protected void populateItem(ListItem item) {
                    Repo repo = (Repo) item.getModelObject();
                    String key = repo.getKey();
                    String href = hrefPrefix + "/" + key + "/";
                    item.add(new ExternalLink("link", href, repo.getKey()));
                    String icon = repo.isCache() ? "repository-cache.png" : "repository.png";
                    item.add(new Image("icon", new ResourceReference("/images/" + icon)));
                }
            });
            return;
        }
        JcrHelper jcr = cc.getJcr();
        ArtifactoryContext context = ContextHelper.get();
        final SecurityHelper security = context.getSecurity();
        final String path = repoPath.getPath();
        List<DirectoryItem> dirItems;
        //List the local repository directory
        dirItems = jcr.doInSession(
                new JcrCallback<List<DirectoryItem>>() {
                    public List<DirectoryItem> doInJcr(JcrSessionWrapper session)
                            throws RepositoryException {
                        JcrFolder dir = (JcrFolder) repo.getFsItem(path, session);
                        List<JcrFsItem> items = dir.getItems();
                        //Sort files by name
                        Collections.sort(items);
                        List<DirectoryItem> dirItems = new ArrayList<DirectoryItem>();
                        //Add the .. link if necessary
                        if (StringUtils.hasLength(path)) {
                            int upDirIdx = path.lastIndexOf('/');
                            String upDirPath;
                            if (upDirIdx > 0) {
                                upDirPath = path.substring(0, upDirIdx);
                            } else {
                                upDirPath = "";
                            }
                            DirectoryItem upDirItem =
                                    new DirectoryItem(DirectoryItem.UP, upDirPath, true);
                            dirItems.add(upDirItem);
                        }
                        for (JcrFsItem item : items) {
                            //Check if we should return the child
                            String itemPath = item.relPath();
                            RepoPath childRepoPath = new RepoPath(item.repoKey(), itemPath);
                            boolean childReader = security.canRead(childRepoPath);
                            if (!childReader) {
                                //Don't bother with stuff that we do not have read access to
                                continue;
                            }
                            String itemName = item.getName();
                            boolean directory = item.isFolder();
                            DirectoryItem directoryItem =
                                    new DirectoryItem(itemName, itemPath, directory);
                            dirItems.add(directoryItem);
                        }
                        return dirItems;
                    }
                }
        );
        add(new ListView("items", dirItems) {
            protected void populateItem(ListItem item) {
                DirectoryItem directoryItem = (DirectoryItem) item.getModelObject();
                String directoryPath = directoryItem.getPath();
                if ("/".equals(directoryPath)) {
                    //Do not repeat / twice for root repo link
                    directoryPath = "";
                }
                String href = hrefPrefix + "/" + repoKey + "/" + directoryPath;
                item.add(new ExternalLink("link", href, directoryItem.getName()));
                item.add(new Image("icon",
                        new ResourceReference("/images/" + directoryItem.getIcon())));
            }
        });
    }

    public String getTitle() {
        return repoPath.toString();
    }

    private static class DirectoryItem implements Serializable {
        private static final long serialVersionUID = 1L;

        static final String UP = "..";

        private final String name;
        private final String path;
        private final boolean directory;
        private final String icon;

        private DirectoryItem(String name, String path, boolean directory) {
            this.name = name;
            this.directory = directory;
            if (directory) {
                icon = "folder.png";
                this.path = path + "/";
            } else {
                this.path = path;
                if (MavenUtils.isJarVariant(name)) {
                    icon = "jar.png";
                } else if (name.equals(UP)) {
                    icon = "parent.png";
                } else if (name.endsWith(".pom")) {
                    icon = "pom.png";
                } else if (name.endsWith(".xml")) {
                    icon = "xml.png";
                } else {
                    icon = "doc.png";
                }
            }
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public boolean isDirectory() {
            return directory;
        }

        public String getIcon() {
            return icon;
        }
    }
}