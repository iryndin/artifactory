/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.cli.common.Command;
import org.artifactory.cli.common.UrlBasedCommand;
import org.artifactory.cli.main.CommandDefinition;
import org.artifactory.cli.rest.RestClient;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * The "Compress" command class. If artifactory is using a Derby database, this command will call the compress table
 * procedure. This command is applicable for Derby only.
 * <p/>
 * e.g.: curl -u admin:password -i -H "Accept: application/xml" -X POST http://localhost:8080/artifactory/api/system/storage
 *
 * @author Noam Tenne
 * @author Yoav Landman
 */
public class CompressCommand extends UrlBasedCommand implements Command {
    private static final Logger log = LoggerFactory.getLogger(CompressCommand.class);


    /**
     * Constructor
     */
    public CompressCommand() {
        super(CommandDefinition.compress);
    }

    /**
     * Executes the command
     */
    public int execute() throws Exception {
        return compress();
    }

    /**
     * Prints the command usage
     */
    public void usage() {
        defaultUsage();
    }

    /**
     * Calls the compress command on the workspace and datastore tables
     *
     * @throws IOException
     * @throws NoSuchMethodException
     */
    private int compress() throws Exception {
        String compressUri = getUrl() + RestClient.COMPRESS_URL;
        log.info("Sending compress command to {} ...", compressUri);
        MultiStatusHolder statusHolder = post(compressUri, null, MultiStatusHolder.class);
        return reportMultiStatusResult(statusHolder);
    }
}
