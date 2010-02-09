package org.artifactory.cli.command;

import org.artifactory.cli.common.Command;
import org.artifactory.cli.common.UrlBasedCommand;
import org.artifactory.cli.main.CommandDefinition;

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
    public void execute() throws Exception {
        String systemUri = getURL() + "system";
        get(systemUri, null);
    }

    /**
     * Prints the usage of the command
     */
    public void usage() {
        defaultUsage();
    }
}
