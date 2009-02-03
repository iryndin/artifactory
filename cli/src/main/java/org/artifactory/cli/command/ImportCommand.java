package org.artifactory.cli.command;

import org.artifactory.api.config.ImportSettings;
import org.artifactory.cli.common.Command;
import org.artifactory.cli.common.UrlBasedCommand;
import org.artifactory.cli.main.CliOption;
import org.artifactory.cli.main.CommandDefinition;

import java.io.File;

/**
 * The "Import" command class
 *
 * @author Noam Tenne
 */
public class ImportCommand extends UrlBasedCommand implements Command {

    /**
     * Default constructor
     */
    public ImportCommand() {
        super(CommandDefinition.imp, CliOption.noMetadata, CliOption.symlinks, CliOption.syncImport, CliOption.verbose,
                CliOption.failFast, CliOption.failIfEmpty);
    }

    /**
     * Executes the command
     *
     * @throws Exception
     */
    public int execute() throws Exception {
        String systemUri = getURL() + "system/import";
        File importFrom = new File(CommandDefinition.imp.getCommandParam().getValue());
        if (importFrom.exists()) {
            importFrom = new File(importFrom.getCanonicalPath());
        }
        ImportSettings settings = new ImportSettings(importFrom);
        settings.setIncludeMetadata(!CliOption.noMetadata.isSet());
        if (CliOption.symlinks.isSet()) {
            settings.setUseSymLinks(true);
            settings.setCopyToWorkingFolder(true);
        }
        if (CliOption.syncImport.isSet()) {
            settings.setUseSymLinks(false);
            settings.setCopyToWorkingFolder(false);
        }
        settings.setVerbose(CliOption.verbose.isSet());
        settings.setFailFast(CliOption.failFast.isSet());
        settings.setFailIfEmpty(CliOption.failIfEmpty.isSet());
        // TODO: The repo list
        //settings.setReposToImport();
        post(systemUri, settings, null);
        return 0;
    }

    /**
     * Prints the usage of the command
     */
    public void usage() {
        defaultUsage();
    }
}
