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

package org.artifactory.cli.main;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.artifactory.cli.command.HelpCommand;
import org.artifactory.cli.common.Command;
import org.artifactory.cli.common.SecurePrompt;
import org.artifactory.cli.common.UrlBasedCommand;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.RemoteCommandException;
import org.slf4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * User: freds
 */
public class ArtAdmin {
    private static final Logger log = LoggerFactory.getLogger(ArtAdmin.class);
    public static boolean DO_SYSTEM_EXIT = true;
    private static final int ERROR_STATUS = 15;

    @SuppressWarnings({"OverlyComplexMethod"})
    public static void main(String[] args) {
        // install the java.utils.logging to slf4j bridge
        SLF4JBridgeHandler.install();

        if (args.length > 0) {
            String commandName = args[0];
            if (!StringUtils.isEmpty(commandName)) {
                commandName = commandName.trim();
            }
            CommandDefinition commandDefinition;
            try {
                commandDefinition = CommandDefinition.get(commandName);
            } catch (Exception e) {
                System.err.println("Command " + commandName + " is unknown.");
                (new HelpCommand()).usage();
                if (DO_SYSTEM_EXIT) {
                    System.exit(ERROR_STATUS);
                }
                return;
            }

            try {
                Command command = commandDefinition.getCommand();
                boolean analysisValid = command.analyzeParameters(args);
                if (!analysisValid) {
                    if (DO_SYSTEM_EXIT) {
                        System.exit(ERROR_STATUS);
                    }
                    return;
                }
                if (command instanceof UrlBasedCommand) {
                    if (!CliOption.username.isSet()) {
                        System.err.println("The '" + commandName + "' command makes use of Artfiactory's REST API.\n" +
                                "Please specify a username.");
                        if (DO_SYSTEM_EXIT) {
                            System.exit(ERROR_STATUS);
                        }
                    }
                    if (!CliOption.password.isSet()) {
                        char[] passwordChars;
                        try {
                            passwordChars = SecurePrompt.readConsoleSecure("Please enter you password: ");
                        } catch (Exception e) {
                            throw new RemoteCommandException(e.getMessage());
                        }
                        String password = String.valueOf(passwordChars);
                        CliOption.password.setValue(password);
                    }
                }
                long start = System.currentTimeMillis();
                int returnCode = command.execute();
                long executionTime = System.currentTimeMillis() - start;
                String executionTimeString = DurationFormatUtils.formatDuration(executionTime, "s.SS");
                log.info("{} finished in {} seconds.", commandName, executionTimeString);
                if (DO_SYSTEM_EXIT) {
                    System.exit(returnCode);
                }
            } catch (RemoteCommandException rme) {
                log.error(rme.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                if (DO_SYSTEM_EXIT) {
                    System.exit(ERROR_STATUS);
                }
            }
        } else {
            System.err.println("No command has been specified. Please specify a command.");
            (new HelpCommand()).usage();
            if (DO_SYSTEM_EXIT) {
                System.exit(ERROR_STATUS);
            }
        }
    }
}
