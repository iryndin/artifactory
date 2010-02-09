/**
 *  Artifactory by jfrog [http://artifactory.jfrog.org]
 *  Copyright (C) 2000-2008 jfrog Ltd.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/> or write to
 *  the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA 02110-1301 USA.
 *
 *  You can also contact jfrog Ltd. at info@jfrog.org.
 *
 *  The interactive user interfaces in modified source and object code versions
 *  of this program must display Appropriate Legal Notices, as required under
 *  Section 5 of the GNU Affero General Public License version 3.
 *
 *  In accordance with Section 7(b) of the GNU Affero General Public License
 *  version 3, these Appropriate Legal Notices must retain the display of the
 *  "Powered by Artifactory" logo. If the display of the logo is not reasonably
 *  feasible for technical reasons, the Appropriate Legal Notices must display
 *  the words "Powered by Artifactory".
 */

package org.artifactory.webapp.actionable.model;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class GlobalRepoActionableItem extends ActionableItemBase
        implements HierarchicActionableItem {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private static final Logger LOGGER = Logger.getLogger(ActionableItemBase.class);

    public String getDisplayName() {
        return "";
    }

    public String getIconRes() {
        return "/images/root.png";
    }

    public Panel createDisplayPanel(String id) {
        return new EmptyPanel(id);
    }

    public List<RepoAwareActionableItem> getChildren(AuthorizationService authService) {
        //Add a tree node for each file repository and local cache repository
        RepositoryService repositoryService = ContextHelper.get().getRepositoryService();
        List<LocalRepoDescriptor> repos = repositoryService.getLocalAndCachedRepoDescriptors();
        List<RepoAwareActionableItem> items = new ArrayList<RepoAwareActionableItem>(repos.size());
        for (LocalRepoDescriptor repo : repos) {
            LocalRepoActionableItem repoActionableItem = new LocalRepoActionableItem(repo);
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