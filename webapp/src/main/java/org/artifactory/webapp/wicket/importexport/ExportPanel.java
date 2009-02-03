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
public class ExportPanel extends WindowPanel {

    @SuppressWarnings({"UnusedDeclaration"})
    private LocalRepo sourceRepo;
    @SuppressWarnings({"UnusedDeclaration"})
    private String exportToPath;

    public ExportPanel(String string) {
        super(string);
        Form exportForm = new Form("exportForm") {
            protected void onSubmit() {
                sourceRepo.export(new File(exportToPath));
            }
        };
        exportForm.setOutputMarkupId(false);
        add(exportForm);
        final IModel sourceRepoModel = new PropertyModel(this, "sourceRepo");
        final CentralConfig cc = getCc();
        final List<LocalRepo> localRepos = cc.getLocalAndCachedRepositories();

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
}
