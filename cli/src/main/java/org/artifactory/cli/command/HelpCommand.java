package org.artifactory.cli.command;

import org.artifactory.cli.common.BaseCommand;
import org.artifactory.cli.main.CommandDefinition;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantsValue;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

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
    public void execute() throws Exception {
        usage();
    }

    /**
     * Prints the usage of the command
     */
    public void usage() {
        if (CommandDefinition.help.getCommandParam().isSet()) {
            CommandDefinition commandDefinition = null;
            try {
                commandDefinition =
                        CommandDefinition.get(CommandDefinition.help.getCommandParam().getValue());
            }
            catch (IllegalArgumentException iae) {
                System.out.println("Error: could not find command parameter");
                printGeneralUsage();
            }
            if (commandDefinition.equals(CommandDefinition.help)) {
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

        URL propertiesURL = ArtifactoryHome.getDefaultArtifactoryPropertiesUrl();
        Properties propertiesFile = new Properties();
        try {
            propertiesFile.load(propertiesURL.openStream());
            String versionNumber = propertiesFile.
                    getProperty(ConstantsValue.artifactoryVersion.getPropertyName());
            String revision = propertiesFile.
                    getProperty(ConstantsValue.artifactoryRevision.getPropertyName());
            if (versionNumber != null) {
                usage.append(", version ").append(versionNumber);
            }
            if (revision != null) {
                usage.append(" (rev. ").append(revision).append(")");
            }
        } catch (IOException e) {
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
        usage.append("Artifactory is a Maven repository.\n");
        usage.append("For additional information, see http://artifactory.jfrog.org\n");
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
}
