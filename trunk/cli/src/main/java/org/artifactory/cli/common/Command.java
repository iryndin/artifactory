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
    void execute() throws Exception;

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
