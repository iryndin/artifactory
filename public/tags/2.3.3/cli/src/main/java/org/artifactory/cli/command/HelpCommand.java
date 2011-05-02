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

import org.artifactory.cli.common.BaseCommand;
import org.artifactory.cli.main.CommandDefinition;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.version.CompoundVersionDetails;

/**
 * The "Help" command class
 *
 * @author Noam Tenne
 */
public class HelpCommand extends BaseCommand {

    /**
     * Default Constructor
     */
    public HelpCommand() {
        super(CommandDefinition.help);
    }

    /**
     * Executes the command
     *
     * @throws Exception
     */
    public int execute() throws Exception {
        usage();
        return 0;
    }

    /**
     * Prints the usage of the command
     */
    public void usage() {
        if (CommandDefinition.help.getCommandParam().isSet()) {
            CommandDefinition commandDefinition;
            String param = CommandDefinition.help.getCommandParam().getValue();
            try {
                commandDefinition = CommandDefinition.get(param);
            } catch (IllegalArgumentException iae) {
                System.out.println("Error: could not find command parameter: " + param);
                printGeneralUsage();
                return;
            }
            //Avoid getting help on help
            if (CommandDefinition.help.equals(commandDefinition)) {
                CommandDefinition.help.getCommandParam().setValue(null);
            }
            commandDefinition.getCommand().usage();
        } else {
            printGeneralUsage();
        }
    }

    private void printGeneralUsage() {
        StringBuilder usage = new StringBuilder();
        usage.append("Artifactory Command Line Interface");

        try {
            CompoundVersionDetails version = new ArtifactoryHome(new NullLog()).getRunningVersionDetails();
            usage.append(", version ").append(version.getVersionName());
            usage.append(" (rev. ").append(version.getRevision()).append(")");
        } catch (Exception e) {
            System.out.println("Error reading properties file. " +
                    "Disabling version number and revision properties.");
        }
        usage.append(".\n");
        usage.append("Usage: artadmin <command> [arg] [options]\n");
        usage.append("Type 'artadmin help <command>' for help on a specific command.\n\n");
        usage.append("Available commands:\n");
        for (CommandDefinition commandDefinition : CommandDefinition.values()) {
            usage.append("  ").append(commandDefinition.getCommandParam().getName()).append(": ")
                    .append(commandDefinition.getCommandParam().getDescription()).append("\n");
        }
        usage.append("\n");
        usage.append("For additional information about Artifactory: http://artifactory.jfrog.org.\n");
        System.out.println(usage.toString());
    }

    /**
     * Overriding parameter analysis, since the common rules do not apply to this specific command
     *
     * @param args The arguments given in the CLI
     * @return boolean Is analysis valid
     */
    @Override
    public boolean analyzeParameters(String[] args) {
        if (args.length > 1) {
            String arg = args[1];
            getCommandArgument().setValue(arg);
        }

        return true;
    }

    public static class NullLog implements ArtifactoryHome.SimpleLog {
        public void log(String message) {
            // ignore
        }
    }
}
