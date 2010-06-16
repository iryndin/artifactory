/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.validation.validator.NumberValidator;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.behavior.collapsible.CollapsibleBehavior;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalRepoChecksumPolicyType;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;

import java.util.Arrays;

import static org.artifactory.descriptor.repo.SnapshotVersionBehavior.*;

/**
 * Local repository configuration panel.
 *
 * @author Yossi Shaul
 */
public class LocalRepoPanel extends RepoConfigCreateUpdatePanel<LocalRepoDescriptor> {

    public LocalRepoPanel(CreateUpdateAction action, LocalRepoDescriptor repoDescriptor,
                          CachingDescriptorHelper cachingDescriptorHelper) {
        super(action, repoDescriptor, cachingDescriptorHelper);
        setWidth(610);
        add(new CssClass("local-repo-config"));

        addBasicSettings(cachingDescriptorHelper);

        addAdvancedSettings();

        addPropertySetsSettings(cachingDescriptorHelper);
    }

    @Override
    public void addAndSaveDescriptor(LocalRepoDescriptor repoDescriptor) {
        CachingDescriptorHelper helper = getCachingDescriptorHelper();
        MutableCentralConfigDescriptor mccd = helper.getModelMutableDescriptor();
        mccd.addLocalRepository(repoDescriptor);
        helper.syncAndSaveLocalRepositories();
    }

    @Override
    public void saveEditDescriptor(LocalRepoDescriptor repoDescriptor) {
        getCachingDescriptorHelper().syncAndSaveLocalRepositories();
    }

    private void addBasicSettings(CachingDescriptorHelper cachingDescriptorHelper) {
        TitledBorder basicSettings = new TitledBorder("basicSettings");
        form.add(basicSettings);

        basicSettings.add(new RepoGeneralSettingsPanel("generalSettings", isCreate(), cachingDescriptorHelper));

        basicSettings.add(new StyledCheckbox("handleReleases"));
        basicSettings.add(new SchemaHelpBubble("handleReleases.help"));

        basicSettings.add(new StyledCheckbox("handleSnapshots"));
        basicSettings.add(new SchemaHelpBubble("handleSnapshots.help"));

        basicSettings.add(new StyledCheckbox("blackedOut"));
        basicSettings.add(new SchemaHelpBubble("blackedOut.help"));
    }

    private void addAdvancedSettings() {
        TitledBorder advancedSettings = new TitledBorder("advancedSettings");
        advancedSettings.add(new CssClass("local-repo-advanced-settings"));
        advancedSettings.add(new CollapsibleBehavior().setResizeModal(true));
        form.add(advancedSettings);

        // maxUniqueSnapshots
        final TextField maxUniqueSnapshots = new MaxUniqueSnapshotsTextField("maxUniqueSnapshots");
        advancedSettings.add(maxUniqueSnapshots);
        advancedSettings.add(new SchemaHelpBubble("maxUniqueSnapshots.help"));

        // snapshotVersionBehavior
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
        advancedSettings.add(snapshotVersionDropDown);
        advancedSettings.add(new SchemaHelpBubble("snapshotVersionBehavior.help"));

        // suppressPomConsistencyChecks
        advancedSettings.add(new StyledCheckbox("suppressPomConsistencyChecks"));
        advancedSettings.add(new SchemaHelpBubble("suppressPomConsistencyChecks.help"));

        advancedSettings.add(new TextArea("notes"));
        advancedSettings.add(new SchemaHelpBubble("notes.help"));

        // snapshotVersionBehavior
        LocalRepoChecksumPolicyType[] checksumPolicyTypes = LocalRepoChecksumPolicyType.values();
        DropDownChoice checksumPolicyDropDown = new DropDownChoice("checksumPolicyType",
                Arrays.asList(checksumPolicyTypes), new ChecksumPolicyChoiceRenderer());
        advancedSettings.add(checksumPolicyDropDown);
        advancedSettings.add(new SchemaHelpBubble("localRepoChecksumPolicyType.help", "checksumPolicyType"));
    }

    private class MaxUniqueSnapshotsTextField extends TextField {
        public MaxUniqueSnapshotsTextField(String id) {
            super(id, Integer.class);
            add(NumberValidator.range(0, Integer.MAX_VALUE));
            setRequired(true);
            setOutputMarkupId(true);
        }

        @Override
        public boolean isEnabled() {
            LocalRepoDescriptor descriptor = getRepoDescriptor();
            SnapshotVersionBehavior snapshotVersionBehavior = descriptor.getSnapshotVersionBehavior();
            boolean isUnique = UNIQUE.equals(snapshotVersionBehavior);
            boolean isDeployer = DEPLOYER.equals(snapshotVersionBehavior);
            return (isUnique || isDeployer);
        }
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

    private static class ChecksumPolicyChoiceRenderer extends ChoiceRenderer {
        @Override
        public Object getDisplayValue(Object object) {
            if (LocalRepoChecksumPolicyType.SERVER.equals(object)) {
                return "Trust server generated checksums";
            } else {
                return "Verify against client checksums";
            }
        }
    }
}
