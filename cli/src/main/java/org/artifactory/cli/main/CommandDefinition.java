/*
 * This file is part of Artifactory.
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

package org.artifactory.cli.main;

import org.artifactory.cli.command.*;
import org.artifactory.cli.common.BaseParam;
import org.artifactory.cli.common.Command;
import org.artifactory.cli.common.Param;

import java.util.Locale;

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
            "Export the full data of a running artifactory instance to a path location on the file-system",
            "export path"),
    imp(
            ImportCommand.class,
            "import",
            "Import a full artifactory from a location on the file-system",
            "import path"),
    compress(
            CompressCommand.class,
            "Compress the (Derby only) tables in order to free up disk space."),
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
        commandName = commandName.toLowerCase(Locale.ENGLISH);
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
