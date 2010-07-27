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

import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusEntry;
import org.artifactory.cli.main.CliOption;
import org.artifactory.cli.main.CommandDefinition;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;

/**
 * The main base class for CLI commands
 *
 * @author Noam Tenne
 */
public abstract class BaseCommand implements Command {
    private static final Logger log = LoggerFactory.getLogger(BaseCommand.class);
    /**
     * The Command definition of the command
     */
    private final CommandDefinition commandDefinition;
    /**
     * Array of the parameters which can be used with this command
     */
    private final CliOption[] optionalParameters;
    /**
     * Core path
     */
    public static final String LIB_ARTIFACTORY_CORE = "WEB-INF/lib/artifactory-core";
    /**
     * War path
     */
    public static final String WEBAPPS_ARTIFACTORY_WAR = "artifactory.war";

    /**
     * Default constructor
     */
    public BaseCommand(CommandDefinition commandDefinition, CliOption... optionalParameters) {
        if (commandDefinition == null) {
            throw new IllegalArgumentException("Command object cannot be null");
        }
        this.commandDefinition = commandDefinition;
        this.optionalParameters = optionalParameters;
    }

    /**
     * Prints the command usage
     */
    protected void defaultUsage() {
        StringBuilder sb = new StringBuilder();
        addCommandDesc(sb);
        addCommandUsage(sb);
        addCommandOptions(sb);
        System.out.println(sb.toString());
    }

    /**
     * Prints the command description
     *
     * @param sb
     */
    protected void addCommandDesc(StringBuilder sb) {
        sb.append(commandDefinition.getCommandParam().getName()).append(": ")
                .append(commandDefinition.getCommandParam().getDescription()).append("\n");
    }

    /**
     * Prints the command usage
     */
    protected void addCommandUsage(StringBuilder sb) {
        sb.append("usage: ").append(commandDefinition.getCommandParam().getName());
        if (commandDefinition.getCommandParam().isNeedExtraParam()) {
            sb.append(" [")
                    .append(commandDefinition.getCommandParam().getParamDescription()).append("]");
        }
        sb.append(" ...\n");
    }

    /**
     * Prints the command options
     */
    protected void addCommandOptions(StringBuilder sb) {
        if ((optionalParameters != null) && (optionalParameters.length > 0)) {
            sb.append("Valid options:\n");
            for (CliOption cliOption : optionalParameters) {
                sb.append("  --").append(cliOption.getName());
                if (cliOption.getParamDescription() != null) {
                    sb.append(" [").append(cliOption.getParamDescription()).append("]");
                }
                sb.append(": ").append(cliOption.getDescription());
                sb.append("\n");
            }
        }
    }

    protected int reportMultiStatusResult(MultiStatusHolder statusHolder) {
        List<StatusEntry> errors = statusHolder.getErrors();
        if (errors.size() > 0) {
            log.info("The command was NOT successfully completed. The following errors were returned: ");
            for (StatusEntry error : errors) {
                log.error("- " + error.getMessage());
            }
        } else {
            log.info("The command was successfully completed!");
        }
        List<StatusEntry> warnings = statusHolder.getWarnings();
        if (warnings.size() > 0) {
            log.info("The command returned the following warnings: ");
            for (StatusEntry warning : warnings) {
                log.error("- " + warning.getMessage());
            }
        }
        return statusHolder.getLastError() == null ? 0 : 1;
    }

    /**
     * Returns the command's name
     *
     * @return Command's name
     */
    public String getName() {
        return getCommandArgument().getName();
    }

    /**
     * Returns the command's argument
     *
     * @return Command's argument
     */
    protected Param getCommandArgument() {
        return commandDefinition.getCommandParam();
    }

    /**
     * Returns one the command's associated CLI options via name. Returns null if there is no option found with the
     * given name.
     *
     * @param optionName The name of the option
     * @return The requested CLI option
     */
    public CliOption getOption(String optionName) {
        for (CliOption optionalParameter : optionalParameters) {
            if (optionalParameter.getName().equalsIgnoreCase(optionName)) {
                return optionalParameter;
            }
        }
        return null;
    }

    /**
     * Returns all the parameters that the command supports
     *
     * @return All optional parameters
     */
    public CliOption[] getOptionalParams() {
        return optionalParameters;
    }

    /**
     * Analyzes the given arguments and extracts the commands, options and arguments
     *
     * @param args Arguments given in the CLI
     * @return Boolean value representing the validity of the given arguments
     */
    public boolean analyzeParameters(String[] args) {
        int i = 0;
        Param argument = getCommandArgument();
        i = checkExtraParam("command", argument, args, i);
        if (i == -1) {
            return false;
        }

        // After this point we have a commandDefinition usage and getOption go to commandDefinition
        for (; i < args.length; i++) {
            String arg = getCleanArgument(args[i]);
            if (!arg.startsWith(OptionInfo.OPTION_PREFIX)) {
                System.out.println("Error parsing parameter " + arg);
                usage();
                return false;
            }
            final String optionName = arg.substring(OptionInfo.OPTION_PREFIX.length());
            Option option = getOption(optionName);
            if (option == null) {
                System.out.println("Unknown argument: " + arg);
                usage();
                return false;
            }

            i = checkExtraParam("option", option, args, i);
            if (i == -1) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks one parameter ahead (to extract the values)
     *
     * @param paramType Param type (<command> or [option])
     * @param param     The class of the certain option
     * @param args      The arguments given in the CLI
     * @param i         The current location of the iterator
     * @return
     */
    private int checkExtraParam(String paramType, Param param, String[] args, int i) {
        if (param.isNeedExtraParam()) {
            i++;
            if (i >= args.length) {
                System.out.println("The " + paramType + " " + param + " needs a parameter: " +
                        param.getParamDescription());
                usage();
                return -1;
            }
            String arg = getCleanArgument(args[i]);
            param.setValue(arg);
            if (param.getValue().startsWith(OptionInfo.OPTION_PREFIX)) {
                // Unset on error
                param.setValue(null);
                System.out.println("The " + paramType + " " + param + " needs a parameter: " +
                        param.getParamDescription());
                usage();
                return -1;
            }
        } else {
            param.set();
        }
        if ("command".equals(paramType)) {
            i++;
        }
        return i;
    }

    /**
     * Returns the given argument after being trimmed of the leading and trailing whitespaces
     *
     * @param argument Argument to trim
     * @return String - Trimmed argument
     */
    private String getCleanArgument(String argument) {
        return argument.trim();
    }
}