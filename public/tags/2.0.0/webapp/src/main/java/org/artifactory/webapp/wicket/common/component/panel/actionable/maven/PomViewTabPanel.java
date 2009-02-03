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
package org.artifactory.webapp.wicket.common.component.panel.actionable.maven;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.wicket.common.component.TextContentPanel;
import org.artifactory.webapp.wicket.common.component.border.fieldset.FieldSetBorder;

/**
 * This tab will be displayed when a pom file is selected from the browse tree.
 *
 * @author Yossi Shaul
 */
public class PomViewTabPanel extends Panel {

    @SpringBean
    private RepositoryService repoService;

    private RepoAwareActionableItem repoItem;

    public PomViewTabPanel(String id, RepoAwareActionableItem repoItem) {
        super(id);
        this.repoItem = repoItem;

        addDependencySection();

        addPomContent();
    }

    private void addDependencySection() {
        FileInfo fileInfo = new FileInfo(repoItem.getRepoPath());
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

        ItemInfo info = repoItem.getItemInfo();
        String content = repoService.getPomContent(info);
        TextContentPanel contentPanel = new TextContentPanel("pomContent");
        contentPanel.setContent(content);
        border.add(contentPanel);
    }
}