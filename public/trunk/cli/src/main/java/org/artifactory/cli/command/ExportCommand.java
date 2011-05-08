/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.cli.command;

import org.artifactory.api.config.ExportSettings;
import org.artifactory.cli.common.Command;
import org.artifactory.cli.common.UrlBasedCommand;
import org.artifactory.cli.main.CliOption;
import org.artifactory.cli.main.CommandDefinition;
import org.artifactory.cli.rest.RestClient;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;

/**
 * The "Export" command class
 *
 * @author Noam Tenne
 */
public class ExportCommand extends UrlBasedCommand implements Command {
    private static final Logger log = LoggerFactory.getLogger(ExportCommand.class);

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
                CliOption.createArchive,
                CliOption.incremental
        );
    }

    /**
     * Executes the command
     *
     * @throws Exception
     */
    public int execute() throws Exception {
        String systemUri = getUrl() + RestClient.EXPORT_URL;
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
        if (CliOption.incremental.isSet()) {
            settings.setIncremental(true);
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
