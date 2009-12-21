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

package org.artifactory.webapp.wicket.page.config.repos;

import org.apache.commons.lang.WordUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.validation.validator.NumberValidator;
import org.artifactory.addon.wicket.PropertiesAddon;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import static org.artifactory.descriptor.repo.SnapshotVersionBehavior.*;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;

import java.util.Arrays;

/**
 * Local repository configuration panel.
 *
 * @author Yossi Shaul
 */
public class LocalRepoPanel extends RepoConfigCreateUpdatePanel<LocalRepoDescriptor> {

    public LocalRepoPanel(CreateUpdateAction action, LocalRepoDescriptor repoDescriptor,
            MutableCentralConfigDescriptor mutableCentralConfig) {
        super(action, repoDescriptor, mutableCentralConfig);

        TitledBorder localRepoFields = new TitledBorder("localRepoFields");
        form.add(localRepoFields);
        localRepoFields.add(new StyledCheckbox("handleReleases"));
        localRepoFields.add(new SchemaHelpBubble("handleReleases.help"));

        localRepoFields.add(new StyledCheckbox("handleSnapshots"));
        localRepoFields.add(new SchemaHelpBubble("handleSnapshots.help"));

        localRepoFields.add(new StyledCheckbox("blackedOut"));
        localRepoFields.add(new SchemaHelpBubble("blackedOut.help"));

        localRepoFields.add(new StyledCheckbox("suppressPomConsistencyChecks"));
        localRepoFields.add(new SchemaHelpBubble("suppressPomConsistencyChecks.help"));

        TitledBorder advanced = new TitledBorder("advanced");
        form.add(advanced);

        final TextField maxUniqueSnapshots = new TextField("maxUniqueSnapshots", Integer.class) {
            @Override
            public boolean isEnabled() {
                return isUniqueSelected();
            }
        };
        maxUniqueSnapshots.add(NumberValidator.range(0, Integer.MAX_VALUE));
        maxUniqueSnapshots.setRequired(true);
        maxUniqueSnapshots.setOutputMarkupPlaceholderTag(true);
        maxUniqueSnapshots.setOutputMarkupId(true);
        advanced.add(maxUniqueSnapshots);
        Label maxUniqueSnapshotsLabel = new Label("maxUniqueSnapshotsLabel", "Max Unique Snapshots");
        maxUniqueSnapshotsLabel.setOutputMarkupPlaceholderTag(true);
        maxUniqueSnapshotsLabel.setOutputMarkupId(true);
        advanced.add(maxUniqueSnapshotsLabel);
        advanced.add(new TextArea("includesPattern"));
        advanced.add(new TextArea("excludesPattern"));

        SchemaHelpBubble maxUniqueSnapshotsHelp = new SchemaHelpBubble("maxUniqueSnapshots.help");
        maxUniqueSnapshotsHelp.setOutputMarkupPlaceholderTag(true);
        maxUniqueSnapshotsHelp.setOutputMarkupId(true);
        advanced.add(maxUniqueSnapshotsHelp);
        advanced.add(new SchemaHelpBubble("includesPattern.help"));
        advanced.add(new SchemaHelpBubble("excludesPattern.help"));
        advanced.add(new SchemaHelpBubble("snapshotVersionBehavior.help"));
        SnapshotVersionBehavior[] versions = SnapshotVersionBehavior.values();
        final DropDownChoice snapshotVersionDropDown =
                new DropDownChoice("snapshotVersionBehavior", Arrays.asList(versions));
        snapshotVersionDropDown.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                LocalRepoDescriptor descriptor = getRepoDescriptor();
                if (NONUNIQUE.equals(descriptor.getSnapshotVersionBehavior())) {
                    descriptor.setMaxUniqueSnapshots(0);
                }
                target.addComponent(maxUniqueSnapshots);
            }
        });
        snapshotVersionDropDown.setChoiceRenderer(new SnapshotVersionChoiceRenderer());

        advanced.add(snapshotVersionDropDown);

        PropertiesAddon propertiesAddon = getAddonsProvider().addonByType(PropertiesAddon.class);
        WebMarkupContainer propertySetsBorder = propertiesAddon.getPropertySetsBorder("setsBorder", "propertySets",
                entity, mutableCentralConfig.getPropertySets());
        form.add(propertySetsBorder);
    }

    @Override
    public void addDescriptor(MutableCentralConfigDescriptor mccd, LocalRepoDescriptor repoDescriptor) {
        mccd.addLocalRepository(repoDescriptor);
    }

    private boolean isUniqueSelected() {
        LocalRepoDescriptor descriptor = getRepoDescriptor();
        SnapshotVersionBehavior snapshotVersionBehavior = descriptor.getSnapshotVersionBehavior();
        boolean isUnique = UNIQUE.equals(snapshotVersionBehavior);
        boolean isDeployer = DEPLOYER.equals(snapshotVersionBehavior);
        return (isUnique || isDeployer);
    }

    private static class SnapshotVersionChoiceRenderer extends ChoiceRenderer {
        @Override
        public Object getDisplayValue(Object object) {
            if (object instanceof SnapshotVersionBehavior) {
                return ((SnapshotVersionBehavior) object).getDisplayName();
            }
            return WordUtils.capitalizeFully(object.toString());
        }
    }
}
