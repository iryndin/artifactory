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

package org.artifactory.webapp.wicket.page.build.actionable;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.build.api.Dependency;
import org.artifactory.webapp.actionable.RepoAwareActionableItemBase;
import org.artifactory.webapp.actionable.action.ShowInTreeAction;
import org.artifactory.webapp.wicket.util.ItemCssClass;

import java.util.List;

/**
 * The published module dependency actionable item
 *
 * @author Noam Y. Tenne
 */
public class ModuleDependencyActionableItem extends RepoAwareActionableItemBase {

    private Dependency dependency;

    /**
     * Main constructor
     *
     * @param repoPath   Repo path of dependency
     * @param dependency Dependency object
     */
    public ModuleDependencyActionableItem(RepoPath repoPath, Dependency dependency) {
        super(repoPath);
        this.dependency = dependency;
    }

    public String getDisplayName() {
        return dependency.getId();
    }

    public Dependency getDependency() {
        return dependency;
    }

    public String getCssClass() {
        return ItemCssClass.doc.getCssClass();
    }

    public void filterActions(AuthorizationService authService) {
        ShowInTreeAction action = new ShowInTreeAction();
        RepoPath repoPath = getRepoPath();
        if ((repoPath == null) || (!authService.canRead(repoPath))) {
            action.setEnabled(false);
        }
        getActions().add(action);
    }

    /**
     * Returns the scopes of the dependency
     *
     * @return Dependency scopes
     */
    public String getDependencyScopes() {
        StringBuilder builder = new StringBuilder();
        List<String> scopes = dependency.getScopes();
        if (scopes != null) {
            for (String scope : scopes) {
                if (StringUtils.isNotBlank(scope)) {
                    int scopeIndex = scopes.indexOf(scope);
                    builder.append(scope);
                    if (scopeIndex < (scopes.size() - 1)) {
                        builder.append(";");
                    }
                }
            }
        }

        return builder.toString();
    }

    /**
     * Returns the dependencies required by this dependency
     *
     * @return Dependencies required for this dependencies
     */
    public String getDependencyRequiredBy() {
        StringBuilder builder = new StringBuilder();
        List<String> requiredByList = dependency.getRequiredBy();
        if (requiredByList != null) {
            for (String requiredBy : requiredByList) {
                if (StringUtils.isNotBlank(requiredBy)) {
                    int requiredByIndex = requiredByList.indexOf(requiredBy);
                    builder.append(requiredBy);
                    if (requiredByIndex < (requiredByList.size() - 1)) {
                        builder.append(";");
                    }
                }
            }
        }

        return builder.toString();
    }
}