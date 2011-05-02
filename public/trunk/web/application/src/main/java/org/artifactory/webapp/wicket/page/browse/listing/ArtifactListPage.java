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
package org.artifactory.webapp.wicket.page.browse.listing;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.servlet.AbortWithWebErrorCodeException;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.BaseBrowsableItem;
import org.artifactory.api.repo.BrowsableItemCriteria;
import org.artifactory.api.repo.RepositoryBrowsingService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.storage.StorageUnit;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.webapp.servlet.RepoFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.apache.commons.lang.StringUtils.rightPad;

/**
 * @author Yoav Aharoni
 */
public class ArtifactListPage extends WebPage {
    public static final String PATH = "_list";

    @SpringBean
    private RepositoryService repositoryService;

    @SpringBean
    private RepositoryBrowsingService repoBrowsingService;

    @SpringBean
    private CentralConfigService centralConfig;

    public ArtifactListPage() {
        setStatelessHint(false);
        setVersioned(false);

        //Retrieve the repository path from the request
        WebRequestCycle requestCycle = (WebRequestCycle) RequestCycle.get();
        WebRequest request = requestCycle.getWebRequest();
        RepoPath repoPath =
                (RepoPath) request.getHttpServletRequest().getAttribute(RepoFilter.ATTR_ARTIFACTORY_REPOSITORY_PATH);
        if (repoPath == null) {
            throw new AbortWithWebErrorCodeException(HttpServletResponse.SC_NOT_FOUND);
        }
        if (StringUtils.isEmpty(repoPath.getRepoKey())) {
            throw new AbortWithWebErrorCodeException(HttpServletResponse.SC_NOT_FOUND);
        }

        addTitle(repoPath);
        Properties requestProps = (Properties) request.getHttpServletRequest()
                .getAttribute(RepoFilter.ATTR_ARTIFACTORY_REQUEST_PROPERTIES);
        addFileList(repoPath, requestProps);
        addAddress(request.getHttpServletRequest());
    }

    private void addTitle(RepoPath repoPath) {
        final String title = String.format("Index of %s/%s", repoPath.getRepoKey(), repoPath.getPath());
        add(new Label("title", title));
        add(new Label("pageTitle", title));
    }

    private void addFileList(final RepoPath repoPath, Properties requestProps) {
        final List<? extends BaseBrowsableItem> items = getItems(repoPath, requestProps);

        // get max name length
        int maxLength = 4;
        for (BaseBrowsableItem baseBrowsableItem : items) {
            maxLength = Math.max(baseBrowsableItem.getName().length(), maxLength);
        }

        // print head
        addTableHead(maxLength);

        // items
        final boolean printParent = StringUtils.isNotEmpty(repoPath.getPath());
        add(new ItemsListView(items, maxLength + 2, printParent));
    }

    private void addTableHead(int maxLength) {
        final StringBuilder head = new StringBuilder();
        head.append(rightPad("Name", maxLength + 2));
        head.append("Last modified      Size");
        add(new Label("head", head.toString()).setEscapeModelStrings(false));
    }

    private void addAddress(HttpServletRequest request) {
        final String version = centralConfig.getVersionInfo().getVersion();
        final String serverName = request.getServerName();
        String address =
                String.format("Artifactory/%s Server at %s Port %s", version, serverName, request.getServerPort());
        add(new Label("address", address));
    }

    private List<? extends BaseBrowsableItem> getItems(RepoPath repoPath, Properties requestProps) {
        List<? extends BaseBrowsableItem> items = Lists.newArrayList();
        try {
            BrowsableItemCriteria criteria = new BrowsableItemCriteria.Builder(repoPath).
                    requestProperties(requestProps).build();
            if (repositoryService.remoteRepoDescriptorByKey(repoPath.getRepoKey()) != null) {
                items = repoBrowsingService.getRemoteRepoBrowsableChildren(criteria);
            } else if (repositoryService.localOrCachedRepoDescriptorByKey(repoPath.getRepoKey()) != null) {
                items = repoBrowsingService.getLocalRepoBrowsableChildren(criteria);
            } else if (repositoryService.virtualRepoDescriptorByKey(repoPath.getRepoKey()) != null) {
                items = repoBrowsingService.getVirtualRepoBrowsableChildren(criteria);
            }
        } catch (Exception e) {
            throw new AbortWithWebErrorCodeException(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
        Collections.sort(items, new ItemInfoComparator());
        return items;
    }

    private static class ItemsListView extends WebComponent {
        private final List<? extends BaseBrowsableItem> items;
        private final int columnSize;
        private final boolean printParent;
        public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.ENGLISH);

        public ItemsListView(List<? extends BaseBrowsableItem> items, int columnSize, boolean printParent) {
            super("list");
            setEscapeModelStrings(false);
            this.items = items;
            this.columnSize = columnSize;
            this.printParent = printParent;
        }

        @Override
        protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
            final Response response = getResponse();
            if (printParent) {
                response.println("<a href=\"../\">../</a>");

            } else if (items.isEmpty()) {
                response.println("No items found.");
                return;
            }

            for (BaseBrowsableItem item : items) {
                String name = item.getName();
                response.write("<a href=\"");
                response.write(name);
                if (item.isFolder()) {
                    response.write("/\"");
                }
                response.write("\">");
                response.write(name);
                if (item.isFolder()) {
                    response.write("/");
                }
                response.write("</a>");
                if (item.isRemote()) {
                    response.write("->");
                }
                response.write(StringUtils.repeat(" ", columnSize - name.length()));
                if (item.getLastModified() > 0) {
                    response.write(DATE_FORMAT.format(item.getLastModified()));
                } else {
                    response.write("  -");
                }
                response.write("  ");

                long size = item.getSize();
                if (item.isFolder() || size <= 0) {
                    response.println("  -");
                } else {
                    response.println(StorageUnit.toReadableString(size));
                }
            }
        }
    }

    private static class ItemInfoComparator implements Comparator<BaseBrowsableItem>, Serializable {

        public int compare(BaseBrowsableItem o1, BaseBrowsableItem o2) {
            final int folderCmp = Boolean.valueOf(o2.isFolder()).compareTo(o1.isFolder());
            if (folderCmp != 0) {
                return folderCmp;
            }
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    }
}
