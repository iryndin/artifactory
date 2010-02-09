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

package org.artifactory.webapp.actionable.action;

import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;

/**
 * This action will display a popup windows with the content of the selected text file.
 *
 * @author Yossi Shaul
 */
public class ViewTextFileAction extends ViewAction {

    @Override
    public void onAction(RepoAwareItemEvent e) {
        RepoAwareActionableItem source = e.getSource();
        ItemInfo itemInfo = source.getItemInfo();
        if (itemInfo.isFolder()) {
            e.getTarget().getPage().error("View action is not applicable on folders");
            return;
        }

        String content = getContent((FileInfo) itemInfo);
        String title = itemInfo.getName();

        displayModalWindow(e, content, title);
    }

    private String getContent(FileInfo fileInfo) {
        String content = getRepoService().getTextFileContent(fileInfo);
        return content;
    }

    @Override
    public String getCssClass() {
        return ViewAction.class.getSimpleName();
    }
}
