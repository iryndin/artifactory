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

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.protocol.http.servlet.AbortWithWebErrorCodeException;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.repo.DirectoryItem;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.webapp.servlet.RequestUtils;
import org.artifactory.webapp.wicket.page.browse.simplebrowser.root.SimpleBrowserRootPage;
import org.artifactory.webapp.wicket.util.ItemCssClass;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.artifactory.api.repo.DirectoryItem.UP;

/**
 * @author Yoav Landman
 */
public class LocalRepoBrowserPanel extends TitledPanel {

    private static final long serialVersionUID = 1L;

    @SpringBean
    private RepositoryService repoService;

    private final RepoPath repoPath;

    public LocalRepoBrowserPanel(String id, final RepoPath repoPath) {
        super(id);
        this.repoPath = repoPath;

        add(new BreadCrumbsPanel("breadCrumbs", repoPath.getId()));

        List<DirectoryItem> dirItems = repoService.getDirectoryItems(repoPath, true);
        if (dirItems == null) {
            throw new AbortWithWebErrorCodeException(HttpServletResponse.SC_NOT_FOUND);
        }
        final String hrefPrefix = RequestUtils.getWicketServletContextUrl();
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
                AbstractLink link;
                if (isEmpty(directoryItem.getItemInfo().getRepoKey())) {
                    link = getRootLink();
                } else {
                    link = new ExternalLink("link", href, directoryItem.getName());
                }
                item.add(link);
                link.add(new org.artifactory.common.wicket.behavior.CssClass(
                        getCssClass(directoryItem)));
            }

            private BookmarkablePageLink getRootLink() {
                return new BookmarkablePageLink("link", SimpleBrowserRootPage.class) {
                    @Override
                    protected void onComponentTagBody(MarkupStream markupStream,
                            ComponentTag openTag) {
                        replaceComponentTagBody(markupStream, openTag, getModelObjectAsString(UP));
                    }
                };
            }
        });
    }

    public String getCssClass(DirectoryItem dirItem) {
        ItemInfo itemInfo = dirItem.getItemInfo();
        if (itemInfo.isFolder()) {
            return ItemCssClass.folder.name();
        } else {
            String path = itemInfo.getRelPath();
            return ItemCssClass.getFileCssClass(path).name();
        }
    }
}