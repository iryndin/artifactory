package org.artifactory.webapp.wicket.page.config.repos;

import org.apache.commons.lang.WordUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.validation.validator.NumberValidator;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import static org.artifactory.descriptor.repo.SnapshotVersionBehavior.DEPLOYER;
import static org.artifactory.descriptor.repo.SnapshotVersionBehavior.UNIQUE;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;

import java.util.Arrays;

/**
 * Local repository configuration panel.
 *
 * @author Yossi Shaul
 */
public class LocalRepoPanel extends RepoConfigCreateUpdatePanel<LocalRepoDescriptor> {
    public LocalRepoPanel(CreateUpdateAction action, LocalRepoDescriptor repoDescriptor) {
        super(action, repoDescriptor);

        TitledBorder localRepoFields = new TitledBorder("localRepoFields");
        form.add(localRepoFields);

        localRepoFields.add(new StyledCheckbox("handleReleases"));
        localRepoFields.add(new SchemaHelpBubble("handleReleases.help"));

        localRepoFields.add(new StyledCheckbox("handleSnapshots"));
        localRepoFields.add(new SchemaHelpBubble("handleSnapshots.help"));

        localRepoFields.add(new StyledCheckbox("blackedOut"));
        localRepoFields.add(new SchemaHelpBubble("blackedOut.help"));

        TitledBorder advanced = new TitledBorder("advanced");
        form.add(advanced);

        final TextField maxUniqueSnapshots = new TextField("maxUniqueSnapshots", Integer.class) {
            @Override
            public boolean isVisible() {
                return isUniqueSelected();
            }
        };
        maxUniqueSnapshots.add(NumberValidator.range(0, Integer.MAX_VALUE));
        maxUniqueSnapshots.setRequired(true);
        maxUniqueSnapshots.setOutputMarkupPlaceholderTag(true);
        maxUniqueSnapshots.setOutputMarkupId(true);
        advanced.add(maxUniqueSnapshots);
        final Label maxUniqueSnapshotsLabel = new Label("maxUniqueSnapshotsLabel", "Max Unique Snapshots") {
            @Override
            public boolean isVisible() {
                return isUniqueSelected();
            }
        };
        maxUniqueSnapshotsLabel.setOutputMarkupPlaceholderTag(true);
        maxUniqueSnapshotsLabel.setOutputMarkupId(true);
        advanced.add(maxUniqueSnapshotsLabel);
        advanced.add(new TextArea("includesPattern"));
        advanced.add(new TextArea("excludesPattern"));

        final SchemaHelpBubble maxUniqueSnapshotsHelp = new SchemaHelpBubble("maxUniqueSnapshots.help") {
            @Override
            public boolean isVisible() {
                return isUniqueSelected();
            }
        };
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
                target.addComponent(maxUniqueSnapshots);
                target.addComponent(maxUniqueSnapshotsLabel);
                target.addComponent(maxUniqueSnapshotsHelp);
            }
        });
        snapshotVersionDropDown.setChoiceRenderer(new SnapshotVersionChoiceRenderer());

        advanced.add(snapshotVersionDropDown);
    }

    @Override
    public void handleCreate(CentralConfigDescriptor descriptor) {
        LocalRepoDescriptor localRepo = getRepoDescriptor();
        getEditingDescriptor().addLocalRepository(localRepo);
    }

    private boolean isUniqueSelected() {
        boolean isUnique = UNIQUE.equals(getRepoDescriptor().getSnapshotVersionBehavior());
        boolean isDeployer = DEPLOYER.equals(getRepoDescriptor().getSnapshotVersionBehavior());
        return (isUnique || isDeployer);
    }

    private class SnapshotVersionChoiceRenderer extends ChoiceRenderer {
        @Override
        public Object getDisplayValue(Object object) {
            if (object instanceof SnapshotVersionBehavior) {
                return ((SnapshotVersionBehavior) object).getDisplayName();
            }
            return WordUtils.capitalizeFully(object.toString());
        }
    }
}
