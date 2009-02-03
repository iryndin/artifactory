package org.artifactory.cli.command;

import org.artifactory.api.config.ExportSettings;
import org.artifactory.cli.common.Command;
import org.artifactory.cli.common.UrlBasedCommand;
import org.artifactory.cli.main.CliOption;
import org.artifactory.cli.main.CommandDefinition;

import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;

/**
 * The "Export" command class
 *
 * @author Noam Tenne
 */
public class ExportCommand extends UrlBasedCommand implements Command {

    /**
     * Default constructor
     */
    public ExportCommand() {
        super(CommandDefinition.export, CliOption.noMetadata, CliOption.createArchive,
                CliOption.bypassFiltering, CliOption.time, CliOption.verbose, CliOption.failFast,
                CliOption.failIfEmpty, CliOption.m2);
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
        if (CliOption.time.isSet()) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
            Date time = new Date(simpleDateFormat.parse(CliOption.time.getValue()).getTime());
            if (time != null) {
                settings.setTime(time);
            }
        }
        if (CliOption.verbose.isSet()) {
            settings.setVerbose(true);
        }
        if (CliOption.failFast.isSet()) {
            settings.setFailFast(true);
        }
        if (CliOption.failIfEmpty.isSet()) {
            settings.setFailIfEmpty(true);
        }
        if (CliOption.m2.isSet()) {
            settings.setM2Compatible(true);
        }

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
