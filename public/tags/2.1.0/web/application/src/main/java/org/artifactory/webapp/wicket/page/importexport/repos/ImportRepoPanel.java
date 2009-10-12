/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.wicket.page.importexport.repos;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.file.browser.button.FileBrowserButton;
import org.artifactory.common.wicket.component.file.path.PathAutoCompleteTextField;
import org.artifactory.common.wicket.component.file.path.PathMask;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;

/**
 * @author Yoav Aharoni
 */
public class ImportRepoPanel extends BasicImportPanel {

    public ImportRepoPanel(String string) {
        super(string);
        Form form = getImportForm();
        PropertyModel pathModel = new PropertyModel(this, "importFromPath");
        final PathAutoCompleteTextField importFromPathTf =
                new PathAutoCompleteTextField("importFromPath", pathModel);
        importFromPathTf.setMask(PathMask.FOLDERS);
        importFromPathTf.setRequired(true);

        StyledCheckbox verboseCheckbox = new StyledCheckbox("verbose", new PropertyModel(this, "verbose"));
        verboseCheckbox.setRequired(false);

        FileBrowserButton browserButton = new FileBrowserButton("browseButton", pathModel) {
            @Override
            protected void onOkClicked(AjaxRequestTarget target) {
                super.onOkClicked(target);
                target.addComponent(importFromPathTf);
            }
        };
        browserButton.setMask(PathMask.FOLDERS);
        form.add(browserButton);
        form.add(importFromPathTf);
        form.add(new HelpBubble("repoSelectHelp", getRepoSelectHelpText()));
        form.add(verboseCheckbox);
        CharSequence systemLogsPage = WicketUtils.mountPathForPage(SystemLogsPage.class);
        form.add(new HelpBubble("verboseHelp",
                "Hint: You can monitor the log in the <a href=\"" + systemLogsPage + "\">'System Logs'</a> page."));

        form.add(new StyledCheckbox("excludeMetadata", new PropertyModel(this, "excludeMetadata")));
        form.add(new HelpBubble("excludeMetadataHelp", "Exclude Artifactory-specific metadata from the export."));
    }

    @Override
    protected void onBeforeImport() {/*Nothing needs doing*/}

    private String getRepoSelectHelpText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Selects where to import the uploaded content.\n");
        sb.append(
                "When importing a single repository, the file structure within the folder you select should be similar to:\n");
        sb.append("SELECTED_DIR\n");
        sb.append(" |\n");
        sb.append(" |--LIB_DIR_1\n");
        sb.append("\n");
        sb.append(
                "But when importing all repositories, the file structure within the folder you select should be similar to:\n");
        sb.append("SELECTED_DIR\n");
        sb.append(" |\n");
        sb.append(" |--REPOSITORY_NAME_DIR_1\n");
        sb.append(" |    |\n");
        sb.append(" |    |--LIB_DIR_1\n");
        sb.append("\n");
        sb.append(
                "When importing all repositories, make sure that the names of the directories that represent\n");
        sb.append(
                "the repositories in the archive, match the names of the target repositories in Artifactory.\n");
        return sb.toString();
    }
}
