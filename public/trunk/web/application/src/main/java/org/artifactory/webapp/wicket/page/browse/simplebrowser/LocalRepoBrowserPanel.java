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

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.protocol.http.servlet.AbortWithWebErrorCodeException;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.repo.BaseBrowsableItem;
import org.artifactory.api.repo.BrowsableItem;
import org.artifactory.api.repo.RepositoryBrowsingService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.webapp.servlet.RequestUtils;
import org.artifactory.webapp.wicket.page.browse.simplebrowser.root.SimpleBrowserRootPage;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author Yoav Landman
 */
public class LocalRepoBrowserPanel extends BaseRepoBrowserPanel {
    private static final Logger log = LoggerFactory.getLogger(LocalRepoBrowserPanel.class);

    private static final long serialVersionUID = 1L;

    @SpringBean
    private RepositoryService repoService;

    @SpringBean
    private RepositoryBrowsingService repoBrowseService;

    public LocalRepoBrowserPanel(String id, final RepoPath repoPath) {
        super(id);

        add(new BreadCrumbsPanel("breadCrumbs", repoPath.getId()));

        List<BaseBrowsableItem> dirItems;
        try {
            dirItems = repoBrowseService.getLocalRepoBrowsableChildren(repoPath);
        } catch (Exception e) {
            log.debug("Exception occurred while trying to get browsable children for repo path " + repoPath, e);
            log.error("Exception occurred while trying to get browsable children for repo path '{}', '{}", repoPath,
                    e.getMessage());
            throw new AbortWithWebErrorCodeException(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
        dirItems.add(0, getPseudoUpLink(repoPath));

        final String hrefPrefix = RequestUtils.getWicketServletContextUrl();
        add(new ListView<BaseBrowsableItem>("items", dirItems) {

            @Override
            protected void populateItem(ListItem<BaseBrowsableItem> listItem) {
                BaseBrowsableItem browsableItem = listItem.getModelObject();

                String itemRelativePath = browsableItem.getRelativePath();
                if ("/".equals(itemRelativePath)) {
                    //Do not repeat / twice for root repo link
                    itemRelativePath = "";
                } else {
                    if (browsableItem.isFolder() && itemRelativePath.length() > 0) {
                        itemRelativePath += "/";
                    }
                }
                String href = hrefPrefix + "/" + repoPath.getRepoKey() + "/" + itemRelativePath;
                AbstractLink link;
                if (isEmpty(browsableItem.getRepoKey())) {
                    link = getRootLink();
                } else {
                    link = new ExternalLink("link", href, browsableItem.getName());
                }
                link.add(new CssClass(getCssClass(browsableItem)));
                listItem.add(link);
            }

            private BookmarkablePageLink getRootLink() {
                return new BookmarkablePageLink<SimpleBrowserRootPage>("link", SimpleBrowserRootPage.class) {
                    @Override
                    protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
                        replaceComponentTagBody(markupStream, openTag, getDefaultModelObjectAsString(BrowsableItem.UP));
                    }
                };
            }
        });
    }


}