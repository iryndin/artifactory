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

import org.artifactory.cli.command.HelpCommand;
import org.artifactory.cli.common.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: freds Date: Aug 12, 2008 Time: 5:36:40 PM
 */
public class ArtifactoryCli {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryCli.class);
    public static boolean DO_SYSTEM_EXIT = true;

    public static void main(String[] args) {

        if (args.length > 0) {
            String commandName = args[0];
            CommandDefinition commandDefinition;
            try {
                commandDefinition = CommandDefinition.get(commandName);
            } catch (Exception e) {
                System.err.println("Command " + commandName + " does not exists!");
                (new HelpCommand()).usage();
                if (DO_SYSTEM_EXIT) {
                    System.exit(-1);
                }
                return;
            }

            try {
                Command command = commandDefinition.getCommand();
                boolean analysisValid = command.analyzeParameters(args);
                if (!analysisValid) {
                    if (DO_SYSTEM_EXIT) {
                        System.exit(-1);
                    }
                    return;
                }
                command.execute();
            } catch (Exception e) {
                e.printStackTrace();
                if (DO_SYSTEM_EXIT) {
                    System.exit(-1);
                }
            }
        } else {
            System.err.println("No command was specified. Please specify a command");
            (new HelpCommand()).usage();
            if (DO_SYSTEM_EXIT) {
                System.exit(-1);
            }
        }
    }
}
