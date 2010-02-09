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
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.behavior.CssClass;
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

    /**
     * Main constructor
     *
     * @param id       ID to assign to the panel
     * @param repoItem Selected repo item
     */
    public PomViewTabPanel(String id, FileActionable repoItem) {
        super(id);
        add(new CssClass("veiw-tab"));

        FileInfo fileInfo = repoItem.getFileInfo();
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(fileInfo.getRepoPath());
        addMavenDependencySection(artifactInfo);
        addIvyDependencySection(artifactInfo);
        addPomContent(fileInfo);
    }

    /**
     * Adds the maven dependency declaration content
     *
     * @param artifactInfo Artifact info to display
     */
    private void addMavenDependencySection(MavenArtifactInfo artifactInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("<dependency>\n");
        sb.append("    <groupId>").append(artifactInfo.getGroupId()).append("</groupId>\n");
        sb.append("    <artifactId>").append(artifactInfo.getArtifactId())
                .append("</artifactId>\n");
        sb.append("    <version>").append(artifactInfo.getVersion()).append("</version>\n");
        sb.append("</dependency>");

        FieldSetBorder border = new FieldSetBorder("mavenDependencyBorder");
        add(border);
        border.add(new TextContentPanel("mavenDependencyDeclaration").setContent(sb.toString()));
    }

    /**
     * Adds the ivy dependency declaration content
     *
     * @param artifactInfo Artifact info to display
     */
    private void addIvyDependencySection(MavenArtifactInfo artifactInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("<dependency org=\"");
        sb.append(artifactInfo.getGroupId()).append("\" ");
        sb.append("name=\"");
        sb.append(artifactInfo.getArtifactId()).append("\" ");
        sb.append("rev=\"");
        sb.append(artifactInfo.getVersion()).append("\" />");

        FieldSetBorder border = new FieldSetBorder("ivyDependencyBorder");
        add(border);
        border.add(new TextContentPanel("ivyDependencyDeclaration").setContent(sb.toString()));
    }

    /**
     * Adds the complete pom display
     *
     * @param fileInfo Pom file info
     */
    public void addPomContent(FileInfo fileInfo) {
        FieldSetBorder border = new FieldSetBorder("pomBorder");
        add(border);

        String content = repoService.getTextFileContent(fileInfo);
        TextContentPanel contentPanel = new TextContentPanel("pomContent");
        contentPanel.setContent(content);
        border.add(contentPanel);
    }
}