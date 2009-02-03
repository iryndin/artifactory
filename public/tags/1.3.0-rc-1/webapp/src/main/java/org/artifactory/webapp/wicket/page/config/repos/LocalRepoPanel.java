package org.artifactory.webapp.wicket.page.config.repos;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
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

        advanced.add(new TextField("maxUniqueSnapshots", Integer.class));
        advanced.add(new TextField("includesPattern"));
        advanced.add(new TextField("excludesPattern"));

        SnapshotVersionBehavior[] versions = SnapshotVersionBehavior.values();
        advanced.add(new DropDownChoice("snapshotVersionBehavior", Arrays.asList(versions)));
        advanced.add(new SchemaHelpBubble("maxUniqueSnapshots.help"));
        advanced.add(new SchemaHelpBubble("includesPattern.help"));
        advanced.add(new SchemaHelpBubble("excludesPattern.help"));
        advanced.add(new SchemaHelpBubble("snapshotVersionBehavior.help"));
    }

    @Override
    public void handleCreate(CentralConfigDescriptor descriptor) {
        LocalRepoDescriptor localRepo = getRepoDescriptor();
        getEditingDescriptor().addLocalRepository(localRepo);
    }
}
