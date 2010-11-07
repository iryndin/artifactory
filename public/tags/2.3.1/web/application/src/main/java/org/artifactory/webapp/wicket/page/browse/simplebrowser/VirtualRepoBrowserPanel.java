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

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
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
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.VirtualBrowsableItem;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.repo.RepoPath;
import org.artifactory.webapp.servlet.RequestUtils;
import org.artifactory.webapp.wicket.page.browse.simplebrowser.root.SimpleBrowserRootPage;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Yoav Landman
 */
public class VirtualRepoBrowserPanel extends TitledPanel {

    private static final long serialVersionUID = 1L;

    @SpringBean
    private RepositoryService repoService;

    public VirtualRepoBrowserPanel(String id, RepoPath repoPath) {
        super(id);
        add(new CssClass("virtual-repo-browser"));
        add(new BreadCrumbsPanel("breadCrumbs", repoPath.getId()));

        final String hrefPrefix = RequestUtils.getWicketServletContextUrl();
        //Try to get a virtual repo
        List<VirtualBrowsableItem> browsableChildren = repoService.getVirtualBrowsableChildren(repoPath, true);
        if (browsableChildren == null) {
            //Return a 404
            throw new AbortWithWebErrorCodeException(HttpServletResponse.SC_NOT_FOUND);
        }

        final String repoKey = repoPath.getRepoKey();

        //Add a table for the dirItems
        DirectoryItemsDataProvider dataProvider = new DirectoryItemsDataProvider(Lists.newArrayList(browsableChildren));
        DataView table = new DataView<VirtualBrowsableItem>("items", dataProvider) {

            @Override
            protected void populateItem(final Item<VirtualBrowsableItem> listItem) {
                VirtualBrowsableItem browsableItem = listItem.getModelObject();

                String relativePath = browsableItem.getRelativePath();
                if ("/".equals(relativePath)) {
                    //Do not repeat / twice for root repo link
                    relativePath = "";
                }

                String itemName = browsableItem.getName();
                AbstractLink itemLink;
                if (VirtualBrowsableItem.UP.equals(relativePath)) {
                    //Up link to the list of repositories
                    itemLink = new BookmarkablePageLink<Class>("link", SimpleBrowserRootPage.class);
                } else {
                    StringBuilder hrefBuilder = new StringBuilder(hrefPrefix).append("/").append(repoKey).
                            append("/").append(relativePath);

                    //Make sure to add a slash at the end of the URI so we always reach the browser
                    if (browsableItem.isFolder() &&
                            ((!StringUtils.isBlank(relativePath)) && (!StringUtils.endsWith(relativePath, "/")))) {
                        hrefBuilder.append("/");
                    }
                    itemLink = new ExternalLink("link", hrefBuilder.toString(), itemName);
                }
                itemLink.add(new CssClass(getCssClass(browsableItem)));
                listItem.add(itemLink);

                final List<String> repoKeys = browsableItem.getRepoKeys();
                final String finalRelativePath = relativePath;
                ListView<String> repositoriesList = new ListView<String>("repositoriesList", repoKeys) {

                    @Override
                    protected void populateItem(ListItem<String> repoKeyListItem) {
                        String repoItemKey = repoKeyListItem.getModelObject();
                        String localRepoHref = hrefPrefix + "/" + repoItemKey + "/" + finalRelativePath;
                        ExternalLink repoItemLink = new ExternalLink("repoKey", localRepoHref, repoItemKey);
                        repoItemLink.add(new CssClass("item-link"));
                        repoKeyListItem.add(repoItemLink);
                    }
                };
                listItem.add(repositoriesList);
                listItem.add(new AttributeModifier("class", true, new AbstractReadOnlyModel() {
                    @Override
                    public Object getObject() {
                        return (listItem.getIndex() % 2 == 0) ? "even" : "odd";
                    }
                }));
            }
        };
        //Add sorting decorator
        add(new OrderByBorder("orderByName", "name", dataProvider));
        add(table);
    }

    private String getCssClass(VirtualBrowsableItem browsableItem) {
        String cssClass;
        if (browsableItem.isFolder()) {
            cssClass = "folder";
        } else {
            String name = browsableItem.getName();
            if (NamingUtils.getMimeType(name).isArchive()) {
                cssClass = "jar";
            } else if (name.equals(VirtualBrowsableItem.UP)) {
                cssClass = "parent";
            } else if (NamingUtils.isPom(name)) {
                cssClass = "pom";
            } else if (NamingUtils.isXml(name)) {
                cssClass = "xml";
            } else {
                cssClass = "doc";
            }
        }

        return cssClass;
    }

    private static class DirectoryItemsDataProvider extends SortableDataProvider<VirtualBrowsableItem> {
        private List<VirtualBrowsableItem> browsableChildren;

        private DirectoryItemsDataProvider(List<VirtualBrowsableItem> browsableChildren) {
            this.browsableChildren = browsableChildren;
            //Set the initial sort direction
            ISortState state = getSortState();
            state.setPropertySortOrder("name", ISortState.ASCENDING);
        }

        public Iterator<? extends VirtualBrowsableItem> iterator(int first, int count) {
            SortParam sortParam = getSort();
            if (sortParam.isAscending()) {
                Collections.sort(browsableChildren);
            } else {
                Collections.sort(browsableChildren, Collections.reverseOrder());
                // now put the up dir first
                VirtualBrowsableItem up = browsableChildren.remove(browsableChildren.size() - 1);
                browsableChildren.add(0, up);
            }
            List<VirtualBrowsableItem> list = browsableChildren.subList(first, first + count);
            return list.iterator();
        }

        public IModel<VirtualBrowsableItem> model(VirtualBrowsableItem object) {
            return new Model<VirtualBrowsableItem>(object);
        }

        public int size() {
            return browsableChildren.size();
        }
    }
}
