package org.artifactory.webapp.wicket.importexport;

import org.apache.log4j.Logger;
import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.LocalRepo;
import org.artifactory.webapp.wicket.ArtifactoryPage;
import org.artifactory.webapp.wicket.widget.AutoCompletePath;
import wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import wicket.markup.html.form.DropDownChoice;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.SubmitLink;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.model.IModel;
import wicket.model.PropertyModel;

import java.io.File;
import java.util.List;

@AuthorizeInstantiation("ADMIN")
public class ImportExportPage extends ArtifactoryPage {

    @SuppressWarnings({"UnusedDeclaration", "UNUSED_SYMBOL"})
    private final static Logger LOGGER = Logger.getLogger(ImportExportPage.class);

    @SuppressWarnings({"UnusedDeclaration"})
    private LocalRepo targetRepo;
    @SuppressWarnings({"UnusedDeclaration"})
    private String importFromPath;

    @SuppressWarnings({"UnusedDeclaration"})
    private LocalRepo sourceRepo;
    @SuppressWarnings({"UnusedDeclaration"})
    private String exportToPath;

    /**
     * Constructor.
     */
    public ImportExportPage() {
        // Create feedback panel
        final FeedbackPanel feedbackPanel = new FeedbackPanel("feedback");
        add(feedbackPanel);
        Form importForm = new Form("importForm") {
            protected void onSubmit() {
                //TODO: [by yl] Import each file seperately to avoid a long running transaction
                File folder = new File(importFromPath);
                targetRepo.importFolder(folder, false);
            }
        };
        importForm.setOutputMarkupId(false);
        add(importForm);
        //Add the dropdown choice for the targetRepo
        final CentralConfig cc = getCc();
        final IModel targetRepoModel = new PropertyModel(this, "targetRepo");
        final List<LocalRepo> localRepos = cc.getLocalRepositories();
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

        Form exportForm = new Form("exportForm") {
            protected void onSubmit() {
                sourceRepo.export(new File(exportToPath));
            }
        };
        exportForm.setOutputMarkupId(false);
        add(exportForm);
        final IModel sourceRepoModel = new PropertyModel(this, "sourceRepo");
        final LocalRepo defaultSource = localRepos.get(0);
        DropDownChoice sourceRepoDdc =
                new DropDownChoice("sourceRepo", sourceRepoModel, localRepos) {
                    @Override
                    protected CharSequence getDefaultChoice(final Object selected) {
                        return defaultSource.toString();
                    }
                };
        //Needed because get getDefaultChoice does not update the actual selection object
        sourceRepoDdc.setModelObject(defaultSource);
        exportForm.add(sourceRepoDdc);
        AutoCompletePath exportToPathTf =
                new AutoCompletePath("exportToPath", new PropertyModel(this, "exportToPath"));
        exportToPathTf.setRequired(true);
        //--importFromPath.add(new ValidateArtifactFormBehavior("onKeyup"));
        exportForm.add(exportToPathTf);
        //Add the import button
        /*Link importButton = new AjaxFallbackLink("import") {
            public void onClick(final AjaxRequestTarget target) {
               targetRepo.importFolder(new File(importFromPath));
            }
        };*/
        SubmitLink exportButton = new SubmitLink("export");
        exportForm.add(exportButton);
    }

    protected String getPageName() {
        return "Local Import Export";
    }
}
