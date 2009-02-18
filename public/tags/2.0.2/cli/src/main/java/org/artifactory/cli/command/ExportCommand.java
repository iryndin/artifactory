package org.artifactory.cli.command;

import org.artifactory.api.config.ExportSettings;
import org.artifactory.cli.common.Command;
import org.artifactory.cli.common.UrlBasedCommand;
import org.artifactory.cli.main.CliOption;
import org.artifactory.cli.main.CommandDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * The "Export" command class
 *
 * @author Noam Tenne
 */
public class ExportCommand extends UrlBasedCommand implements Command {
    private final static Logger log = LoggerFactory.getLogger(ExportCommand.class);

    /**
     * Default constructor
     */
    public ExportCommand() {
        super(
                CommandDefinition.export,
                CliOption.m2,
                CliOption.noMetadata,
                CliOption.verbose,
                CliOption.failOnError,
                CliOption.failIfEmpty,
                CliOption.bypassFiltering,
                CliOption.createArchive
        );
    }

    /**
     * Executes the command
     *
     * @throws Exception
     */
    public int execute() throws Exception {
        String systemUri = getURL() + "system/export";
        File exportTo = new File(CommandDefinition.export.getCommandParam().getValue());
        if (exportTo.exists()) {
            exportTo = new File(exportTo.getCanonicalPath());
        }
        ExportSettings settings = new ExportSettings(exportTo);
        if (CliOption.noMetadata.isSet()) {
            settings.setIncludeMetadata(false);
        }
        if (CliOption.createArchive.isSet()) {
            settings.setCreateArchive(true);
        }
        if (CliOption.bypassFiltering.isSet()) {
            settings.setIgnoreRepositoryFilteringRulesOn(true);
        }
        if (CliOption.verbose.isSet()) {
            settings.setVerbose(true);
        }
        if (CliOption.failOnError.isSet()) {
            settings.setFailFast(true);
        }
        if (CliOption.failIfEmpty.isSet()) {
            settings.setFailIfEmpty(true);
        }
        if (CliOption.m2.isSet()) {
            settings.setM2Compatible(true);
        }

        log.info("Sending export request to server path: {}", exportTo.getPath());

        // TODO: The repo list
        //settings.setReposToExport();
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
