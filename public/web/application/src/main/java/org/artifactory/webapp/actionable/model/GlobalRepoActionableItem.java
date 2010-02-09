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

package org.artifactory.webapp.actionable.model;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.ActionableItemBase;
import org.artifactory.webapp.wicket.util.ItemCssClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class GlobalRepoActionableItem extends ActionableItemBase implements HierarchicActionableItem {

    private boolean compactAllowed;

    public boolean isCompactAllowed() {
        return compactAllowed;
    }

    public void setCompactAllowed(boolean compactAllowed) {
        this.compactAllowed = compactAllowed;
    }

    public String getDisplayName() {
        return "";
    }

    public String getCssClass() {
        return ItemCssClass.root.getCssClass();
    }

    public Panel newItemDetailsPanel(String id) {
        return new EmptyPanel(id);
    }

    public List<ActionableItem> getChildren(AuthorizationService authService) {
        //Add a tree node for each file repository and local cache repository
        RepositoryService repositoryService = ContextHelper.get().getRepositoryService();
        List<LocalRepoDescriptor> repos = repositoryService.getLocalAndCachedRepoDescriptors();
        List<ActionableItem> items = new ArrayList<ActionableItem>(repos.size());
        for (LocalRepoDescriptor repo : repos) {
            LocalRepoActionableItem repoActionableItem = new LocalRepoActionableItem(repo);
            repoActionableItem.setCompactAllowed(isCompactAllowed());
            items.add(repoActionableItem);
        }
        return items;
    }

    public boolean hasChildren(AuthorizationService authService) {
        return true;
    }

    public void filterActions(AuthorizationService authService) {
    }
}