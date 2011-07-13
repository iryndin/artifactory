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

import org.artifactory.api.config.ImportSettings;
import org.artifactory.cli.common.Command;
import org.artifactory.cli.common.UrlBasedCommand;
import org.artifactory.cli.main.CliOption;
import org.artifactory.cli.main.CommandDefinition;
import org.artifactory.cli.rest.RestClient;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;

/**
 * The "Import" command class
 *
 * @author Noam Tenne
 */
public class ImportCommand extends UrlBasedCommand implements Command {
    private static final Logger log = LoggerFactory.getLogger(ImportCommand.class);

    /**
     * Default constructor
     */
    public ImportCommand() {
        super(
                CommandDefinition.imp,
                CliOption.verbose,
                CliOption.noMetadata,
                CliOption.failOnError,
                CliOption.failIfEmpty);
    }

    /**
     * Executes the command
     *
     * @throws Exception
     */
    public int execute() throws Exception {
        String systemUri = getUrl() + RestClient.IMPORT_URL;
        File importFrom = new File(CommandDefinition.imp.getCommandParam().getValue());
        if (importFrom.exists()) {
            importFrom = new File(importFrom.getCanonicalPath());
        }
        ImportSettings settings = new ImportSettings(importFrom);
        settings.setIncludeMetadata(!CliOption.noMetadata.isSet());
        settings.setVerbose(CliOption.verbose.isSet());
        settings.setFailFast(CliOption.failOnError.isSet());
        settings.setFailIfEmpty(CliOption.failIfEmpty.isSet());

        log.info("Sending import request to server from path: {}", importFrom.getPath());

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
