package org.artifactory.cli.main;

import org.artifactory.cli.command.CompressCommand;
import org.artifactory.cli.command.ConfigurationCommand;
import org.artifactory.cli.command.DumpCommand;
import org.artifactory.cli.command.ExportCommand;
import org.artifactory.cli.command.HelpCommand;
import org.artifactory.cli.command.ImportCommand;
import org.artifactory.cli.command.InfoCommand;
import org.artifactory.cli.command.SecurityCommand;
import org.artifactory.cli.common.BaseParam;
import org.artifactory.cli.common.Command;
import org.artifactory.cli.common.Param;

/**
 * Enumeration which holdes the different CLI commands and their specs
 *
 * @author Noam Tenne
 */
public enum CommandDefinition {
    help(
            HelpCommand.class,
            "The help message",
            "the command help"),
    info(
            InfoCommand.class,
            "Get system information"),
    export(
            ExportCommand.class,
            "Export a running artifactory instance data into host destination path",
            "host destination path"),
    imp(
            ImportCommand.class,
            "import",
            "Import full system from host path",
            "host path"),
    dump(
            DumpCommand.class,
            "Dump the database of an older version of an offline artifactory instance to the latest export format",
            "artifactory home folder"),
    compress(
            CompressCommand.class,
            "Compress the (Derby only) tables in order to free up disk space.",
            "artifactory home folder"),
    security(
            SecurityCommand.class,
            "Display or update the security definitions using a security configuration file."),
    configuration(
            ConfigurationCommand.class,
            "Print or update the artifactory configuration using a configuration file.");

    /**
     * The class of the command
     */
    private final Class<? extends Command> commandClass;
    /**
     * The parameter class of the command
     */
    private final Param commandParam;

    /**
     * Constructor
     *
     * @param commandClass Command class
     * @param desc         Command description
     */
    CommandDefinition(Class<? extends Command> commandClass, String desc) {
        this.commandClass = commandClass;
        this.commandParam = new BaseParam(name(), desc, false, "");
    }

    /**
     * Constructor
     *
     * @param commandClass Command class
     * @param desc         Command description
     * @param argDesc      Description of argument
     */
    CommandDefinition(Class<? extends Command> commandClass, String desc, String argDesc) {
        this(commandClass, null, desc, argDesc);
    }

    /**
     * Constructor
     *
     * @param commandClass Command class
     * @param name         Command name
     * @param desc         Command description
     * @param argDesc      Description of argument
     */
    CommandDefinition(Class<? extends Command> commandClass, String name, String desc, String argDesc) {
        if (argDesc == null) {
            throw new IllegalArgumentException("Argument description cannot be null");
        }
        this.commandClass = commandClass;
        this.commandParam = new BaseParam(name != null ? name : name(), desc, true, argDesc);
    }

    /**
     * Returns the command class
     *
     * @return Command The command class
     */
    public Command getCommand() {
        try {
            return commandClass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the command parameter class
     *
     * @return Param command parameter
     */
    public Param getCommandParam() {
        return commandParam;
    }

    /**
     * Returns a command definition via the command name
     *
     * @param commandName Name of command
     * @return CommandDefinition The requested command
     */
    public static CommandDefinition get(String commandName) {
        commandName = commandName.toLowerCase();
        try {
            return CommandDefinition.valueOf(commandName);
        } catch (IllegalArgumentException e) {
            // Try to find inside command param names
            for (CommandDefinition commandDefinition : CommandDefinition.values()) {
                if (commandDefinition.getCommandParam().getName().equals(commandName)) {
                    return commandDefinition;
                }
            }
            throw e;
        }
    }
}
