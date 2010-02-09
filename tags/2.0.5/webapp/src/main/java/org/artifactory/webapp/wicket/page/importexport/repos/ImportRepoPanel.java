/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.wicket.page.importexport.repos;

import org.apache.commons.lang.SystemUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.common.component.file.browser.button.FileBrowserButton;
import org.artifactory.webapp.wicket.common.component.file.path.PathAutoCompleteTextField;
import org.artifactory.webapp.wicket.common.component.file.path.PathMask;
import org.artifactory.webapp.wicket.common.component.help.HelpBubble;

/**
 * @author Yoav Aharoni
 */
public class ImportRepoPanel extends BasicImportPanel {

    final StyledCheckbox copyCheckbox;
    final StyledCheckbox symLinkCheckbox;

    public ImportRepoPanel(String string) {
        super(string);
        copyCheckbox = new StyledCheckbox("copyFiles", new PropertyModel(this, "copyFiles"));
        copyCheckbox.setOutputMarkupId(true);

        symLinkCheckbox = new StyledCheckbox("useSymLinks", new PropertyModel(this, "useSymLinks"));
        symLinkCheckbox.setOutputMarkupId(true);

        Form form = getImportForm();
        PropertyModel pathModel = new PropertyModel(this, "importFromPath");
        final PathAutoCompleteTextField importFromPathTf =
                new PathAutoCompleteTextField("importFromPath", pathModel);
        importFromPathTf.setMask(PathMask.FOLDERS);
        importFromPathTf.setRequired(true);
        importFromPathTf.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateCheckboxes(target);
            }
        });

        StyledCheckbox verboseCheckbox = new StyledCheckbox("verbose", new PropertyModel(this, "verbose"));
        verboseCheckbox.setRequired(false);

        copyCheckbox.setEnabled(false);
        copyCheckbox.setRequired(false);
        copyCheckbox.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (copyCheckbox.isChecked() && canUseSymLink()) {
                    symLinkCheckbox.setEnabled(true);
                } else {
                    symLinkCheckbox.setEnabled(false);
                    symLinkCheckbox.setModelObject(Boolean.FALSE);
                }
                target.addComponent(symLinkCheckbox);
            }
        });

        symLinkCheckbox.setOutputMarkupId(true);
        symLinkCheckbox.setEnabled(false);
        symLinkCheckbox.setRequired(false);

        FileBrowserButton browserButton = new FileBrowserButton("browseButton", pathModel) {
            @Override
            protected void onOkClicked(AjaxRequestTarget target) {
                super.onOkClicked(target);
                target.addComponent(importFromPathTf);
                updateCheckboxes(target);
            }
        };
        browserButton.setMask(PathMask.FOLDERS);
        form.add(browserButton);
        form.add(importFromPathTf);
        form.add(new HelpBubble("repoSelectHelp", getRepoSelectHelpText()));
        form.add(verboseCheckbox);
        form.add(new HelpBubble("verboseHelp", "Hint: You can monitor the log in the 'System Logs' page."));
        form.add(copyCheckbox);
        form.add(symLinkCheckbox);

        form.add(new StyledCheckbox("includeMetadata", new PropertyModel(this, "includeMetadata")));
        form.add(new HelpBubble("includeMetadataHelp", "Include Artifactory-specific metadata as part of the export."));
    }

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
                "The repositories in the archive, match the names of the targeted repositories in the application.\n");
        return sb.toString();
    }

    private void updateCheckboxes(AjaxRequestTarget target) {
        if ((getImportFromPath() != null) && getImportFromPath().exists()) {
            copyCheckbox.setEnabled(true);
        } else {
            copyCheckbox.setEnabled(false);
            copyCheckbox.setModelObject(Boolean.FALSE);
            symLinkCheckbox.setEnabled(false);
            symLinkCheckbox.setModelObject(Boolean.FALSE);
        }
        target.addComponent(copyCheckbox);
        target.addComponent(symLinkCheckbox);
    }

    private boolean canUseSymLink() {
        boolean usingWindows = SystemUtils.IS_OS_WINDOWS;
        return (!usingWindows);
    }
}
