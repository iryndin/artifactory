/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.browse.simplebrowser;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.OrderByBorder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.servlet.AbortWithWebErrorCodeException;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.VirtualRepoItem;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.mime.MimeType;
import org.artifactory.webapp.servlet.RequestUtils;
import org.artifactory.webapp.wicket.page.browse.simplebrowser.root.SimpleBrowserRootPage;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Yoav Landman
 */
public class VirtualRepoBrowserPanel extends TitledPanel {

    private static final long serialVersionUID = 1L;

    private SortedMap<String, DirectoryItem> dirItems = new TreeMap<String, DirectoryItem>();

    @SpringBean
    private RepositoryService repoService;

    public VirtualRepoBrowserPanel(String id, RepoPath repoPath) {
        super(id);
        add(new CssClass("virtual-repo-browser"));
        add(new BreadCrumbsPanel("breadCrumbs", repoPath.getId()));

        final String virtualRepoKey = repoPath.getRepoKey();

        final String hrefPrefix = RequestUtils.getWicketServletContextUrl();
        //Try to get a virtual repo
        VirtualRepoDescriptor virtualRepo = repoService.virtualRepoDescriptorByKey(virtualRepoKey);
        if (virtualRepo == null) {
            //Return a 404
            throw new AbortWithWebErrorCodeException(HttpServletResponse.SC_NOT_FOUND);
        }

        //Collect the items under the virtual directory viewed from all local repositories
        List<VirtualRepoItem> result = repoService.getVirtualRepoItems(repoPath);

        for (VirtualRepoItem item : result) {
            DirectoryItem directoryItem = new DirectoryItem(item);
            dirItems.put(item.getPath(), directoryItem);
        }

        //Finally, add the '..' link if necessary
        addUpLink(repoPath);

        //Add a table for the dirItems
        final DirectoryItemsDataProvider dataProvider = new DirectoryItemsDataProvider("name");
        final DataView table = new DataView("items", dataProvider) {
            //Add each item with its icon and link
            @Override
            protected void populateItem(final Item item) {
                final DirectoryItem dirItem = (DirectoryItem) item.getModelObject();
                String directoryPath = dirItem.getPath();
                if ("/".equals(directoryPath)) {
                    //Do not repeat / twice for root repo link
                    directoryPath = "";
                }
                String name = dirItem.getName();
                AbstractLink link;
                if (DirectoryItem.UP.equals(directoryPath)) {
                    //Up link to the list of repositories
                    link = new BookmarkablePageLink("link", SimpleBrowserRootPage.class);
                } else {
                    String href = hrefPrefix + "/" + virtualRepoKey + "/" + directoryPath;
                    link = new ExternalLink("link", href, name);
                }
                item.add(link);
                link.add(new CssClass(dirItem.getCssClass()));

                final List<String> repoKeys = dirItem.getRepoKeys();
                final String finalDirectoryPath = directoryPath;
                ListView repositoriesList = new ListView("repositoriesList", repoKeys) {
                    @Override
                    protected void populateItem(ListItem item) {
                        String key = (String) item.getModelObject();
                        String href = hrefPrefix + "/" + key + "/" + finalDirectoryPath;
                        ExternalLink link = new ExternalLink("repoKey", href, key);
                        link.add(new CssClass("item-link"));
                        item.add(link);
                    }
                };
                item.add(repositoriesList);
                item.add(new AttributeModifier("class", true, new AbstractReadOnlyModel() {
                    @Override
                    public Object getObject() {
                        return (item.getIndex() % 2 == 0) ? "even" : "odd";
                    }
                }));
            }
        };
        //Add sorting decorator
        add(new OrderByBorder("orderByName", "name", dataProvider));
        add(table);
    }

    private void addUpLink(RepoPath repoPath) {
        final String path = repoPath.getPath();
        DirectoryItem upDirItem;
        if (StringUtils.hasLength(path)) {
            int upDirIdx = path.lastIndexOf('/');
            String upDirPath;
            if (upDirIdx > 0) {
                upDirPath = path.substring(0, upDirIdx);
            } else {
                upDirPath = "";
            }
            upDirItem = new DirectoryItem(DirectoryItem.UP, upDirPath, true, null);
        } else {
            // up link for the root repo dir
            upDirItem = new DirectoryItem(DirectoryItem.UP, DirectoryItem.UP, true, null);
        }
        dirItems.put(DirectoryItem.UP, upDirItem);
    }

    public static class DirectoryItem implements Serializable, Comparable {
        private static final long serialVersionUID = 1L;

        static final String UP = "..";

        private final String name;
        private final String path;
        private final boolean folder;
        private final String cssClass;
        private final List<String> repoKeys;

        private DirectoryItem(VirtualRepoItem child) {
            this(child.getName(), child.getPath(), child.isFolder(), child.getRepoKeys());
        }

        private DirectoryItem(String name, String path, boolean folder, List<String> repoKeys) {
            this.name = name;
            this.folder = folder;
            // TODO: Duplicate from LocalRepoBrowserPanel
            if (folder) {
                cssClass = "folder";
                //DO not change the path for upo, as it is later used in comparissons
                this.path = UP.equals(path) ? path : path + "/";
            } else {
                this.path = path;
                MimeType ct = NamingUtils.getContentType(name);
                if (ct.isArchive()) {
                    cssClass = "jar";
                } else if (name.equals(UP)) {
                    cssClass = "parent";
                } else if (NamingUtils.isPom(name)) {
                    cssClass = "pom";
                } else if (ct.isXml()) {
                    cssClass = "xml";
                } else {
                    cssClass = "doc";
                }
            }
            if (repoKeys != null) {
                this.repoKeys = repoKeys;
            } else {
                this.repoKeys = new ArrayList<String>();
            }
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public boolean isFolder() {
            return folder;
        }

        public String getCssClass() {
            return cssClass;
        }

        public List<String> getRepoKeys() {
            return repoKeys;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DirectoryItem item = (DirectoryItem) o;
            return path.equals(item.path);
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }

        public int compareTo(Object o) {
            DirectoryItem other = (DirectoryItem) o;
            if (this.name.equals(UP)) {
                // up should always appear first
                return -1;
            } else {
                return this.path.compareTo(other.path);
            }
        }
    }

    private class DirectoryItemsDataProvider extends SortableDataProvider {

        private DirectoryItemsDataProvider(String prop) {
            //Set the initial sort direction
            ISortState state = getSortState();
            state.setPropertySortOrder(prop, ISortState.ASCENDING);
        }

        public Iterator iterator(int first, int count) {
            List<DirectoryItem> data = new ArrayList<DirectoryItem>(dirItems.values());
            SortParam sortParam = getSort();
            if (sortParam.isAscending()) {
                Collections.sort(data);
            } else {
                Collections.sort(data, Collections.reverseOrder());
                // now put the up dir first
                DirectoryItem up = data.remove(data.size() - 1);
                data.add(0, up);
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
