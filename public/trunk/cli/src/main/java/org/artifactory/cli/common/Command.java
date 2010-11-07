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

package org.artifactory.cli.common;

import org.artifactory.cli.main.CliOption;

/**
 * The main interface for CLI commands
 *
 * @author Noam Tenne
 */
public interface Command {

    /**
     * Executes the command
     *
     * @throws Exception
     */
    int execute() throws Exception;

    /**
     * Returns a certain CLI Option of the command via the option name
     *
     * @param optionName The name of the requested option
     * @return CliOption Requested option
     */
    CliOption getOption(String optionName);

    /**
     * Returns the usage description of the selected command
     */
    void usage();

    /**
     * Returns the validity of the arguments which were passed
     *
     * @param args Arguments given in CLI
     * @return boolean Are arguments valid
     */
    boolean analyzeParameters(String[] args);
}
