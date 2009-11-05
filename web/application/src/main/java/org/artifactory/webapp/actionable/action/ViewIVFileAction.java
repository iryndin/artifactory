package org.artifactory.webapp.actionable.action;

import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;

/**
 * @author Eli Givoni
 */
public class ViewIVFileAction extends ViewAction {


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
