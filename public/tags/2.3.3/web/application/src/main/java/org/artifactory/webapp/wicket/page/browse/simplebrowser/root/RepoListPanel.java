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

package org.artifactory.webapp.wicket.page.browse.simplebrowser.root;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.descriptor.repo.RepoBaseDescriptor;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.webapp.servlet.RequestUtils;
import org.artifactory.webapp.wicket.util.ItemCssClass;

import java.util.List;

/**
 * @author Yoav Landman
 */
public class RepoListPanel extends TitledPanel {
    private static final long serialVersionUID = 1L;


    public RepoListPanel(String id, List<? extends RepoBaseDescriptor> repoDescriptorList) {
        super(id);
        final String hrefPrefix = RequestUtils.getWicketServletContextUrl();
        add(new ListView<RepoBaseDescriptor>("repos", repoDescriptorList) {
            @Override
            protected void populateItem(ListItem<RepoBaseDescriptor> item) {
                RepoBaseDescriptor repo = item.getModelObject();
                String key = repo.getKey();

                Component browseLink = new ExternalLink("link",
                        hrefPrefix + "/" + ArtifactoryRequest.SIMPLE_BROWSING_PATH + "/" + key + "/", key);
                String cssClass = ItemCssClass.getRepoDescriptorCssClass(repo);
                browseLink.add(new CssClass(cssClass));
                item.add(browseLink);

                final ExternalLink listLink = new ExternalLink("listLink", hrefPrefix + "/list/" + key + "/", " ");
                listLink.add(new SimpleAttributeModifier("title", "Directory Listing for " + key));
                listLink.add(new CssClass("ext-link"));
                item.add(listLink);
            }
        });
    }

    @Override
    public String getTitle() {
        return getString(getId(), null);
    }


}