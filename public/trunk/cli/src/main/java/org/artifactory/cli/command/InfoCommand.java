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

import org.artifactory.cli.common.Command;
import org.artifactory.cli.common.UrlBasedCommand;
import org.artifactory.cli.main.CommandDefinition;
import org.artifactory.cli.rest.RestClient;

/**
 * The "Info" command class
 *
 * @author Noam Tenne
 */
public class InfoCommand extends UrlBasedCommand implements Command {

    /**
     * Default constructor
     */
    public InfoCommand() {
        super(CommandDefinition.info);
    }

    /**
     * Executes the command
     *
     * @throws Exception
     */
    public int execute() throws Exception {
        String systemUri = getUrl() + RestClient.SYSTEM_URL;
        get(systemUri, null);
        return 0;
    }

    /**
     * Prints the usage of the command
     */
    public void usage() {
        defaultUsage();
    }
}
