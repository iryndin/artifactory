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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.webapp.wicket.common.component.file.browser.button.FileBrowserButton;
import org.artifactory.webapp.wicket.common.component.file.path.PathAutoCompleteTextField;
import org.artifactory.webapp.wicket.common.component.file.path.PathMask;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;

/**
 * @author Yoav Aharoni
 */
public class ImportRepoPanel extends BasicImportPanel {

    public ImportRepoPanel(String string) {
        super(string);

        PropertyModel pathModel = new PropertyModel(this, "importFromPath");
        final PathAutoCompleteTextField importFromPathTf =
                new PathAutoCompleteTextField("importFromPath", pathModel);
        importFromPathTf.setMask(PathMask.FOLDERS);
        importFromPathTf.setRequired(true);

        Form form = getImportForm();
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
        form.add(new SchemaHelpBubble("repoSelectHelp", "", getRepoSelectHelpText()));
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

}
