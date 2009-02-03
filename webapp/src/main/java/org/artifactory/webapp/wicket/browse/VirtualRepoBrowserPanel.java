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
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.OrderByBorder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.servlet.AbortWithWebErrorCodeException;
import org.artifactory.config.CentralConfig;
import org.artifactory.jcr.JcrFolder;
import org.artifactory.jcr.JcrFsItem;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.security.RepoPath;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.webapp.wicket.components.SecuredPageLink;
import org.artifactory.webapp.wicket.components.panel.TitlePanel;
import org.artifactory.webapp.wicket.utils.ComparablePropertySorter;
import org.artifactory.webapp.wicket.utils.ServletUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Yoav Landman
 */
public class VirtualRepoBrowserPanel extends TitlePanel {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(VirtualRepoBrowserPanel.class);

    private final RepoPath repoPath;
    private SortedMap<String, DirectoryItem> dirItems = new TreeMap<String, DirectoryItem>();

    public VirtualRepoBrowserPanel(String id, RepoPath repoPath) {
        super(id);
        this.repoPath = repoPath;
        final String virtualRepoKey = repoPath.getRepoKey();
        CentralConfig cc = CentralConfig.get();
        final String hrefPrefix = ServletUtils.getServletContextUrl();
        //Try to get a virtual repo
        VirtualRepo virtualRepo = cc.virtualRepositoryByKey(virtualRepoKey);
        if (virtualRepo == null) {
            //Return a 404
            throw new AbortWithWebErrorCodeException(HttpServletResponse.SC_NOT_FOUND);
        }
        //Collect the items under the virtual directory viewed from all local repositories
        List<LocalRepo> repoList = virtualRepo.getLocalAndCachedRepositories();
        ArtifactoryContext context = ContextHelper.get();
        final SecurityHelper security = context.getSecurity();
        final String path = repoPath.getPath();
        for (final LocalRepo repo : repoList) {
            if (!repo.itemExists(path)) {
                continue;
            }
            JcrFolder dir = (JcrFolder) repo.getFsItem(path);
            List<JcrFsItem> items = dir.getItems();
            for (JcrFsItem item : items) {
                //Security - check that we can return the child
                String itemPath = item.relPath();
                RepoPath childRepoPath = new RepoPath(item.repoKey(), itemPath);
                boolean childReader = security.canRead(childRepoPath);
                if (!childReader) {
                    //Don't bother with stuff that we do not have read access to
                    continue;
                }
                DirectoryItem directoryItem;
                //Check if we already have this item
                if (!dirItems.containsKey(itemPath)) {
                    String itemName = item.getName();
                    boolean directory = item.isFolder();
                    directoryItem = new DirectoryItem(itemName, itemPath, directory);
                    dirItems.put(itemPath, directoryItem);
                } else {
                    directoryItem = dirItems.get(itemPath);
                }
                //Add the current repo to the list of repositories
                String key = repo.getKey();
                directoryItem.getRepoKeys().add(key);
            }
            //Finally, add the '..' link if necessary
            if (StringUtils.hasLength(path)) {
                int upDirIdx = path.lastIndexOf('/');
                String upDirPath;
                if (upDirIdx > 0) {
                    upDirPath = path.substring(0, upDirIdx);
                } else {
                    upDirPath = "";
                }
                DirectoryItem upDirItem = new DirectoryItem(DirectoryItem.UP, upDirPath, true);
                dirItems.put(DirectoryItem.UP, upDirItem);
            } else {
                //Add up link for the root repo dir
                DirectoryItem upDirItem = new DirectoryItem(
                        DirectoryItem.UP, DirectoryItem.UP, true);
                dirItems.put(DirectoryItem.UP, upDirItem);
            }
        }
        //Add a table for the dirItems
        final DirectoryItemsDataProvider dataProvider = new DirectoryItemsDataProvider("name");
        final DataView table = new DataView("items", dataProvider) {
            //Add each item with its icon and link
            protected void populateItem(final Item item) {
                final DirectoryItem dirItem = (DirectoryItem) item.getModelObject();
                String directoryPath = dirItem.getPath();
                if ("/".equals(directoryPath)) {
                    //Do not repeat / twice for root repo link
                    directoryPath = "";
                }
                String name = dirItem.getName();
                if (DirectoryItem.UP.equals(directoryPath)) {
                    //Up link to the list of repositories
                    item.add(new SecuredPageLink("link", VirtualRepoListPage.class));
                } else {
                    String href = hrefPrefix + "/" + virtualRepoKey + "/" + directoryPath;
                    item.add(new ExternalLink("link", href, name));
                }
                item.add(new Image("icon", new ResourceReference("/images/" + dirItem.getIcon())));

                final List<String> repoKeys = dirItem.getRepoKeys();
                final String finalDirectoryPath = directoryPath;
                ListView repositoriesList = new ListView("repositoriesList", repoKeys) {
                    protected void populateItem(ListItem item) {
                        String key = (String) item.getModelObject();
                        String href = hrefPrefix + "/" + key + "/" + finalDirectoryPath;
                        ExternalLink link = new ExternalLink("repoKey", href, key);
                        link.add(new AttributeAppender("class", new Model("cellItemLink"), " "));
                        item.add(link);
                    }
                };
                item.add(repositoriesList);
                item.add(new AttributeModifier("class", true, new AbstractReadOnlyModel() {
                    public Object getObject() {
                        return (item.getIndex() % 2 == 1) ? "even" : "odd";
                    }
                }));
            }
        };
        //Add sorting decorator
        add(new OrderByBorder("orderByName", "name", dataProvider));
        add(table);
    }

    public String getTitle() {
        return repoPath.toString();
    }

    public static class DirectoryItem implements Serializable {
        private static final long serialVersionUID = 1L;

        static final String UP = "..";

        private final String name;
        private final String path;
        private final boolean directory;
        private final String icon;
        private List<String> repoKeys = new ArrayList<String>();

        private DirectoryItem(String name, String path, boolean directory) {
            this.name = name;
            this.directory = directory;
            if (directory) {
                icon = "folder.png";
                //DO not change the path for upo, as it is later used in comparissons
                this.path = UP.equals(path) ? path : path + "/";
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

        public List<String> getRepoKeys() {
            return repoKeys;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DirectoryItem item = (DirectoryItem) o;
            return !(path != null ? !path.equals(item.path) : item.path != null);

        }

        public int hashCode() {
            int result;
            result = (name != null ? name.hashCode() : 0);
            result = 31 * result + (path != null ? path.hashCode() : 0);
            result = 31 * result + (directory ? 1 : 0);
            result = 31 * result + (icon != null ? icon.hashCode() : 0);
            result = 31 * result + (repoKeys != null ? repoKeys.hashCode() : 0);
            return result;
        }
    }

    private class DirectoryItemsDataProvider extends SortableDataProvider {


        private DirectoryItemsDataProvider(String prop) {
            //Set the initial sort direction
            ISortState state = getSortState();
            state.setPropertySortOrder(prop, ISortState.ASCENDING);
        }

        @SuppressWarnings({"unchecked"})
        public Iterator iterator(int first, int count) {
            ArrayList data = new ArrayList(dirItems.values());
            ComparablePropertySorter<DirectoryItem> sorter =
                    new ComparablePropertySorter<DirectoryItem>(DirectoryItem.class);
            SortParam sp = getSort();
            if (sp != null) {
                sorter.sort(data, sp, DirectoryItem.UP);
            }
            List<DirectoryItem> list = data.subList(first, first + count);
            return list.iterator();
        }

        public int size() {
            return dirItems.size();
        }

        public IModel model(Object object) {
            return new Model((DirectoryItem) object);
        }
    }
}