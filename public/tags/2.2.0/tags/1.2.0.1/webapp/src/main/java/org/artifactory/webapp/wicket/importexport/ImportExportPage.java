package org.artifactory.webapp.wicket.importexport;

import org.apache.log4j.Logger;
import org.artifactory.webapp.wicket.ArtifactoryPage;
import wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import wicket.markup.html.panel.FeedbackPanel;

@AuthorizeInstantiation("ADMIN")
public class ImportExportPage extends ArtifactoryPage {

    @SuppressWarnings({"UnusedDeclaration", "UNUSED_SYMBOL"})
    private final static Logger LOGGER = Logger.getLogger(ImportExportPage.class);


    /**
     * Constructor.
     */
    public ImportExportPage() {
        add(new FeedbackPanel("feedback"));
        add(new ImportPanel("importPanel"));
        add(new ExportPanel("exportPanel"));
    }

    protected String getPageName() {
        return "Local Import Export";
    }
}
