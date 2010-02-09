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
    help(HelpCommand.class, "The help message", true, "the command help"),
    info(InfoCommand.class, "Get system information"),
    export(ExportCommand.class, "Export artifactory data to destination path", true,
            "destination path"),
    imp(ImportCommand.class, "import", "Import full system from import path", true,
            "import from path"),
    dump(DumpCommand.class, "Dump the database of an older version of Artifactory", true,
            "Artifactory home folder"),
    compress(CompressCommand.class, "Compress the database tables in order to free up disk space.",
            true,
            "Artifactory home folder"),
    security(SecurityCommand.class, "Prints or updates the security security configuration file."),
    configuration(ConfigurationCommand.class, "Prints or updates the configuration file.");

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
     * @param extraArg     Extra arguments (if needed as well as global ones)
     * @param argDesc      Description of argument
     */
    CommandDefinition(Class<? extends Command> commandClass, String desc, boolean extraArg,
            String argDesc) {
        this.commandClass = commandClass;
        this.commandParam = new BaseParam(name(), desc, extraArg, argDesc);
    }

    /**
     * Constructor
     *
     * @param commandClass Command class
     * @param name         Command name
     * @param desc         Command description
     * @param extraArg     Extra arguments (if needed as well as global ones)
     * @param argDesc      Description of argument
     */
    CommandDefinition(Class<? extends Command> commandClass, String name, String desc,
            boolean extraArg, String argDesc) {
        this.commandClass = commandClass;
        this.commandParam = new BaseParam(name, desc, extraArg, argDesc);
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
