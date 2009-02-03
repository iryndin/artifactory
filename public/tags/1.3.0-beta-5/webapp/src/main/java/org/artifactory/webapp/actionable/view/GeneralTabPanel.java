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

package org.artifactory.webapp.actionable.view;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.webapp.actionable.model.RepoAwareActionableItem;
import org.artifactory.webapp.wicket.component.LabeledValue;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class GeneralTabPanel extends Panel {

    public GeneralTabPanel(String id, RepoAwareActionableItem item) {
        super(id);
        String name = item.getDisplayName();
        LabeledValue nameLabel = new LabeledValue("name", "Name: ", name);
        add(nameLabel);
        ItemInfo itemInfo = item.getItemInfo();
        LabeledValue sizeLabel = new LabeledValue("size", "Size: ");
        add(sizeLabel);
        LabeledValue ageLabel = new LabeledValue("age", "Age: ");
        add(ageLabel);
        //Hack
        LabeledValue groupIdLabel = new LabeledValue("groupId", "GroupId: ");
        add(groupIdLabel);
        LabeledValue artifactIdLabel = new LabeledValue("artifactId", "ArtifactId: ");
        add(artifactIdLabel);
        LabeledValue versionLabel = new LabeledValue("version", "Version: ");
        add(versionLabel);
        LabeledValue deployedByLabel =
                new LabeledValue("deployed-by", "Deployed by: ", itemInfo.getModifiedBy());
        add(deployedByLabel);
        if (itemInfo.isFolder()) {
            ageLabel.setVisible(false);
            sizeLabel.setVisible(false);
            groupIdLabel.setVisible(false);
            artifactIdLabel.setVisible(false);
            versionLabel.setVisible(false);
        } else {
            FileInfo file = (FileInfo) itemInfo;
            MavenArtifactInfo mavenInfo =
                    ContextHelper.get().getRepositoryService().getMavenArtifactInfo(itemInfo);
            long size = file.getSize();
            //If we are looking at a cached item, check the expiry from the remote repository
            String ageStr = "non-cached";
            long age = file.getAge();
            if (age > 0) {
                ageStr = DurationFormatUtils.formatDuration(age, "d'd' H'h' m'm' s's'");
            }
            ageLabel.setValue(ageStr);
            sizeLabel.setValue(FileUtils.byteCountToDisplaySize(size));
            groupIdLabel.setValue(mavenInfo.getGroupId());
            artifactIdLabel.setValue(mavenInfo.getArtifactId());
            versionLabel.setValue(mavenInfo.getVersion());
        }
    }
}