/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.cli.main;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.artifactory.cli.command.HelpCommand;
import org.artifactory.cli.common.Command;
import org.artifactory.cli.common.RemoteCommandException;
import org.artifactory.cli.common.SecurePrompt;
import org.artifactory.cli.common.UrlBasedCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * User: freds Date: Aug 12, 2008 Time: 5:36:40 PM
 */
public class ArtifactoryCli {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryCli.class);
    public static boolean DO_SYSTEM_EXIT = true;
    private static final int ERROR_STATUS = 15;

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
                        System.err.println("The command you have executed makes use of Artfiactory's REST API.\n" +
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
                log.info("{} finished ({} secs)", commandName, executionTimeString);
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
