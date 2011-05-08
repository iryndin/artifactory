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
import org.artifactory.api.repo.BaseBrowsableItem;
import org.artifactory.api.repo.BrowsableItemCriteria;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.repo.RepositoryBrowsingService;
import org.artifactory.api.repo.VirtualBrowsableItem;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.webapp.servlet.RequestUtils;
import org.artifactory.webapp.wicket.page.browse.simplebrowser.root.SimpleBrowserRootPage;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.artifactory.request.ArtifactoryRequest.SIMPLE_BROWSING_PATH;

/**
 * @author Yoav Landman
 */
public class VirtualRepoBrowserPanel extends RemoteBrowsableRepoPanel {
    private static final Logger log = LoggerFactory.getLogger(VirtualRepoBrowserPanel.class);

    private static final long serialVersionUID = 1L;

    @SpringBean
    private RepositoryBrowsingService repoBrowsingService;

    public VirtualRepoBrowserPanel(String id, RepoPath repoPath, Properties requestProps) {
        super(id);
        add(new CssClass("virtual-repo-browser"));
        add(new BreadCrumbsPanel("breadCrumbs", repoPath.getId()));

        final String hrefPrefix = RequestUtils.getWicketServletContextUrl();
        //Try to get a virtual repo
        List<BaseBrowsableItem> browsableChildren;
        try {
            BrowsableItemCriteria criteria = new BrowsableItemCriteria.Builder(repoPath).requestProperties(
                    requestProps).build();
            browsableChildren = repoBrowsingService.getVirtualRepoBrowsableChildren(criteria);
        } catch (Exception e) {
            log.debug("Exception occurred while trying to get browsable children for repo path " + repoPath, e);
            throw new AbortWithWebErrorCodeException(HttpServletResponse.SC_NOT_FOUND);
        }
        browsableChildren.add(getPseudoUpLink(repoPath));
        final String repoKey = repoPath.getRepoKey();

        //Add a table for the dirItems
        DirectoryItemsDataProvider dataProvider = new DirectoryItemsDataProvider(Lists.newArrayList(browsableChildren));
        DataView table = new DataView<BaseBrowsableItem>("items", dataProvider) {

            @Override
            protected void populateItem(final Item<BaseBrowsableItem> listItem) {
                VirtualBrowsableItem browsableItem = (VirtualBrowsableItem) listItem.getModelObject();

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
                    StringBuilder hrefBuilder = new StringBuilder(hrefPrefix).append("/").append(SIMPLE_BROWSING_PATH)
                            .append("/").append(repoKey).append("/").append(relativePath);
                    //Make sure to add a slash at the end of the URI so we always reach the browser
                    if (browsableItem.isFolder() &&
                            ((!StringUtils.isBlank(relativePath)) && (!StringUtils.endsWith(relativePath, "/")))) {
                        hrefBuilder.append("/");
                    }
                    itemLink = new ExternalLink("link", hrefBuilder.toString(), itemName);
                }
                itemLink.add(new CssClass(getCssClass(browsableItem)));
                listItem.add(itemLink);
                addGlobeIcon(listItem, browsableItem.isRemote());
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

    /**
     * Creates a virtual repo "up directory"\.. browsable item
     *
     * @param startingRelativePath Relative path of starting point
     * @return Virtual up-dir Browsable item
     */
    @Override
    protected BaseBrowsableItem getPseudoUpLink(RepoPath repoPath) {
        String startingRelativePath = repoPath.getPath();
        VirtualBrowsableItem upDirItem;
        if (org.apache.commons.lang.StringUtils.isNotBlank(startingRelativePath)) {
            int upDirIdx = startingRelativePath.lastIndexOf('/');
            String upDirPath;
            if (upDirIdx > 0) {
                upDirPath = startingRelativePath.substring(0, upDirIdx);
            } else {
                upDirPath = "";
            }
            upDirItem = new VirtualBrowsableItem(VirtualBrowsableItem.UP, true, 0, 0, 0,
                    new RepoPathImpl("", upDirPath), Lists.<String>newArrayList());
        } else {
            // up link for the root repo dir
            upDirItem = new VirtualBrowsableItem(VirtualBrowsableItem.UP, true, 0, 0, 0,
                    new RepoPathImpl("", VirtualBrowsableItem.UP), Lists.<String>newArrayList());
        }
        return upDirItem;
    }


    @Override
    public String getCssClass(BaseBrowsableItem browsableItem) {
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

    private static class DirectoryItemsDataProvider extends SortableDataProvider<BaseBrowsableItem> {
        private List<BaseBrowsableItem> browsableChildren;

        private DirectoryItemsDataProvider(List<BaseBrowsableItem> browsableChildren) {
            this.browsableChildren = browsableChildren;
            //Set the initial sort direction
            ISortState state = getSortState();
            state.setPropertySortOrder("name", ISortState.ASCENDING);
        }

        public Iterator<? extends BaseBrowsableItem> iterator(int first, int count) {
            SortParam sortParam = getSort();
            if (sortParam.isAscending()) {
                Collections.sort(browsableChildren);
            } else {
                Collections.sort(browsableChildren, Collections.reverseOrder());
                // now put the up dir first
                BaseBrowsableItem up = browsableChildren.remove(browsableChildren.size() - 1);
                browsableChildren.add(0, up);
            }
            List<BaseBrowsableItem> list = browsableChildren.subList(first, first + count);
            return list.iterator();
        }

        public IModel<BaseBrowsableItem> model(BaseBrowsableItem object) {
            return new Model<BaseBrowsableItem>(object);
        }

        public int size() {
            return browsableChildren.size();
        }
    }
}
