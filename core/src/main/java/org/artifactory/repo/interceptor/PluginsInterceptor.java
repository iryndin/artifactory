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

package org.artifactory.repo.interceptor;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.plugin.PluginsAddon;
import org.artifactory.addon.plugin.storage.AfterCopyAction;
import org.artifactory.addon.plugin.storage.AfterCreateAction;
import org.artifactory.addon.plugin.storage.AfterDeleteAction;
import org.artifactory.addon.plugin.storage.AfterMoveAction;
import org.artifactory.addon.plugin.storage.BeforeCopyAction;
import org.artifactory.addon.plugin.storage.BeforeCreateAction;
import org.artifactory.addon.plugin.storage.BeforeDeleteAction;
import org.artifactory.addon.plugin.storage.BeforeMoveAction;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;

import javax.inject.Inject;

/**
 * An interceptor that executes plugin scripts
 *
 * @author Yoav Landman
 */
public class PluginsInterceptor extends StorageInterceptorAdapter {

    @Inject
    AddonsManager addonsManager;

    @Override
    public void beforeCreate(JcrFsItem fsItem, MutableStatusHolder statusHolder) {
        getPluginsAddon().execPluginActions(BeforeCreateAction.class, null, fsItem.getInfo());
    }

    @Override
    public void afterCreate(JcrFsItem fsItem, MutableStatusHolder statusHolder) {
        getPluginsAddon().execPluginActions(AfterCreateAction.class, null, fsItem.getInfo());
    }

    @Override
    public void beforeDelete(JcrFsItem fsItem, MutableStatusHolder statusHolder) {
        getPluginsAddon().execPluginActions(BeforeDeleteAction.class, null, fsItem.getInfo());
    }

    @Override
    public void afterDelete(JcrFsItem fsItem, MutableStatusHolder statusHolder) {
        getPluginsAddon().execPluginActions(AfterDeleteAction.class, null, fsItem.getInfo());
    }

    @Override
    public void beforeMove(JcrFsItem sourceItem, RepoPath targetRepoPath, MutableStatusHolder statusHolder,
            Properties properties) {
        getPluginsAddon().execPluginActions(BeforeMoveAction.class, null, sourceItem.getInfo(), targetRepoPath);
    }


    @Override
    public void afterMove(JcrFsItem sourceItem, JcrFsItem targetItem, MutableStatusHolder statusHolder,
            Properties properties) {
        getPluginsAddon()
                .execPluginActions(AfterMoveAction.class, null, sourceItem.getInfo(), nullOrRepoPath(targetItem));
    }

    @Override
    public void beforeCopy(JcrFsItem sourceItem, RepoPath targetRepoPath, MutableStatusHolder statusHolder,
            Properties properties) {
        getPluginsAddon().execPluginActions(BeforeCopyAction.class, null, sourceItem.getInfo(), targetRepoPath);
    }

    @Override
    public void afterCopy(JcrFsItem sourceItem, JcrFsItem targetItem, MutableStatusHolder statusHolder,
            Properties properties) {
        getPluginsAddon()
                .execPluginActions(AfterCopyAction.class, null, sourceItem.getInfo(), nullOrRepoPath(targetItem));
    }

    private RepoPath nullOrRepoPath(JcrFsItem targetItem) {
        return targetItem != null ? targetItem.getRepoPath() : null;
    }

    private PluginsAddon getPluginsAddon() {
        return addonsManager.addonByType(PluginsAddon.class);
    }
}
