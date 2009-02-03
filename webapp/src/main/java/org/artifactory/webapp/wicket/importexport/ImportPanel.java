package org.artifactory.webapp.wicket.importexport;

import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.LocalRepo;
import org.artifactory.webapp.wicket.widget.AutoCompletePath;
import org.artifactory.webapp.wicket.window.WindowPanel;
import wicket.markup.html.form.DropDownChoice;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.SubmitLink;
import wicket.model.IModel;
import wicket.model.PropertyModel;

import java.io.File;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class ImportPanel extends WindowPanel {

    @SuppressWarnings({"UnusedDeclaration"})
    private LocalRepo targetRepo;
    @SuppressWarnings({"UnusedDeclaration"})
    private String importFromPath;

    public ImportPanel(String string) {
        super(string);
        Form importForm = new Form("importForm") {
            protected void onSubmit() {
                //Import each file seperately to avoid a long running transaction
                File folder = new File(importFromPath);
                targetRepo.importFolder(folder, false);
            }
        };

        importForm.setOutputMarkupId(false);
        add(importForm);
        //Add the dropdown choice for the targetRepo
        final CentralConfig cc = getCc();
        final IModel targetRepoModel = new PropertyModel(this, "targetRepo");
        final List<LocalRepo> localRepos = cc.getLocalAndCachedRepositories();
        final LocalRepo defaultTarget = localRepos.get(0);
        DropDownChoice targetRepoDdc =
                new DropDownChoice("targetRepo", targetRepoModel, localRepos) {
                    @Override
                    protected CharSequence getDefaultChoice(final Object selected) {
                        return defaultTarget.toString();
                    }
                };
        //Needed because get getDefaultChoice does not update the actual selection object
        targetRepoDdc.setModelObject(defaultTarget);
        importForm.add(targetRepoDdc);
        AutoCompletePath importFromPathTf =
                new AutoCompletePath("importFromPath", new PropertyModel(this, "importFromPath"));
        importFromPathTf.setRequired(true);
        //--importFromPath.add(new ValidateArtifactFormBehavior("onKeyup"));
        importForm.add(importFromPathTf);
        //Add the import button
        /*Link importButton = new AjaxFallbackLink("import") {
            public void onClick(final AjaxRequestTarget target) {
               targetRepo.importFolder(new File(importFromPath));
            }
        };*/
        SubmitLink importButton = new SubmitLink("import");
        importForm.add(importButton);
    }
}
