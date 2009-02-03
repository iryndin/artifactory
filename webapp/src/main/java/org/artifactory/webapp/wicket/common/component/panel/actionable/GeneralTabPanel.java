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
package org.artifactory.webapp.wicket.common.component.panel.actionable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.action.RepoAwareItemAction;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;
import org.artifactory.webapp.actionable.model.LocalRepoActionableItem;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.LabeledValue;
import org.artifactory.webapp.wicket.common.component.border.fieldset.FieldSetBorder;
import org.artifactory.webapp.wicket.common.component.panel.actionable.general.ChecksumsPanel;
import org.artifactory.webapp.wicket.common.component.panel.actionable.general.DistributionManagementPanel;
import org.artifactory.webapp.wicket.common.component.panel.actionable.general.VirtualRepoListPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class GeneralTabPanel extends Panel {

    private RepoAwareActionableItem repoItem;

    public GeneralTabPanel(String id, RepoAwareActionableItem repoItem) {
        super(id);
        this.repoItem = repoItem;

        addGeneralInfo();

        if (shouldDisplayDistributionManagement()) {
            add(new DistributionManagementPanel("ditributionManagament", repoItem));
        } else {
            add(new WebMarkupContainer("ditributionManagament"));
        }

        addActions(repoItem);

        add(new VirtualRepoListPanel("virtualRepoList", repoItem));
    }

    private boolean shouldDisplayDistributionManagement() {
        if (repoItem instanceof LocalRepoActionableItem) {
            // display distribution mgmt only if the repo handles releases or snapshots
            LocalRepoDescriptor localRepo = repoItem.getRepo();
            return localRepo.isHandleReleases() || localRepo.isHandleSnapshots();
        } else {
            return false;
        }
    }

    private void addActions(RepoAwareActionableItem repoItem) {
        Set<ItemAction> actions = repoItem.getActions();
        final List<ItemAction> actionList = new ArrayList<ItemAction>(actions.size());
        //Filter enabled actions
        for (ItemAction action : actions) {
            if (action.isEnabled()) {
                actionList.add(action);
            }
        }

        FieldSetBorder actionBorder = new FieldSetBorder("actionBorder") {
            @Override
            public boolean isVisible() {
                return super.isVisible() && !actionList.isEmpty();
            }
        };
        add(actionBorder);

        actionBorder.add(new ListView("action", actionList) {
            @Override
            protected void populateItem(ListItem item) {
                ItemAction action = (ItemAction) item.getModelObject();
                ActionLink link = new ActionLink("link", action);
                link.add(new CssClass(action.getCssClass()));
                item.add(link);
                link.setEnabled(action.isEnabled());
            }
        });
    }

    private void addGeneralInfo() {

        FieldSetBorder infoBorder = new FieldSetBorder("infoBorder");
        add(infoBorder);

        LabeledValue nameLabel = new LabeledValue("name", "Name: ", repoItem.getDisplayName());
        infoBorder.add(nameLabel);

        ItemInfo itemInfo = repoItem.getItemInfo();

        LabeledValue sizeLabel = new LabeledValue("size", "Size: ");
        infoBorder.add(sizeLabel);

        LabeledValue ageLabel = new LabeledValue("age", "Age: ");
        infoBorder.add(ageLabel);

        //Hack
        LabeledValue groupIdLabel = new LabeledValue("groupId", "GroupId: ");
        infoBorder.add(groupIdLabel);

        LabeledValue artifactIdLabel = new LabeledValue("artifactId", "ArtifactId: ");
        infoBorder.add(artifactIdLabel);

        LabeledValue versionLabel = new LabeledValue("version", "Version: ");
        infoBorder.add(versionLabel);

        infoBorder.add(new LabeledValue("deployed-by", "Deployed by: ",
                itemInfo.getInernalXmlInfo().getModifiedBy()));

        //Add markup container in case we need to set the checksum panel
        WebMarkupContainer checksumsContainer = new WebMarkupContainer("checksums");
        add(checksumsContainer);

        // disable/enable and set info according to the node type
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

            //Replace markup container with checksum panel
            Panel checksumPanel = new ChecksumsPanel("checksums", file);
            checksumsContainer.replaceWith(checksumPanel);
        }
    }

    public class ActionLink extends AjaxLink {

        private ItemAction action;

        public ActionLink(String id, ItemAction action) {
            super(id);
            this.action = action;
            add(new Label("name", action.getName()));
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            RepoAwareItemEvent event = new RepoAwareItemEvent(
                    repoItem, (RepoAwareItemAction) action, target);
            action.actionPerformed(event);
        }

        @Override
        protected IAjaxCallDecorator getAjaxCallDecorator() {
            return new AjaxCallDecorator() {
                @Override
                public CharSequence decorateScript(CharSequence script) {
                    CharSequence confirmationPrefix = action.getConfirmationMessage();
                    if (confirmationPrefix != null) {
                        return "if (confirm('" + confirmationPrefix + " " +
                                repoItem.getDisplayName() + "?')) {" +
                                script + "} else { return false; }";
                    }
                    return script;
                }
            };
        }
    }
}