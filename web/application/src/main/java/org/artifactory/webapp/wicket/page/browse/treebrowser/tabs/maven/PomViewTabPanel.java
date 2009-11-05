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
package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.maven;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FileInfoImpl;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.component.TextContentPanel;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;
import org.artifactory.webapp.actionable.FileActionable;

/**
 * This tab will be displayed when a pom file is selected from the browse tree.
 *
 * @author Yossi Shaul
 */
public class PomViewTabPanel extends Panel {

    @SpringBean
    private RepositoryService repoService;

    private FileActionable repoItem;

    public PomViewTabPanel(String id, FileActionable repoItem) {
        super(id);
        this.repoItem = repoItem;

        addDependencySection();

        addPomContent();
    }

    private void addDependencySection() {
        FileInfo fileInfo = new FileInfoImpl(repoItem.getRepoPath());
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(fileInfo.getRepoPath());

        StringBuilder sb = new StringBuilder();
        sb.append("<dependency>\n");
        sb.append("    <groupId>").append(artifactInfo.getGroupId()).append("</groupId>\n");
        sb.append("    <artifactId>").append(artifactInfo.getArtifactId())
                .append("</artifactId>\n");
        sb.append("    <version>").append(artifactInfo.getVersion()).append("</version>\n");
        sb.append("</dependency>");

        FieldSetBorder border = new FieldSetBorder("dependencyBorder");
        add(border);
        border.add(new TextContentPanel("dependencyDeclaration").setContent(sb.toString()));
    }

    public void addPomContent() {
        FieldSetBorder border = new FieldSetBorder("pomBorder");
        add(border);

        String content = repoService.getTextFileContent(repoItem.getFileInfo());
        TextContentPanel contentPanel = new TextContentPanel("pomContent");
        contentPanel.setContent(content);
        border.add(contentPanel);
    }
}