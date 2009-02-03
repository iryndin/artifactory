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
package org.artifactory.cli;

/**
 * @author freds
 * @date Sep 1, 2008
 */
public abstract class OptionParser {
    public void checkExclusive(Option opt1, Option opt2) {
        if (opt1.isSet() && opt2.isSet()) {
            // param are exclusive
            System.out.println("Parameter " + opt1.argValue() +
                    " and " + opt2.argValue() +
                    " cannot be specified together!");
            usage();
        }
    }

    /**
     * You never return from usage since it throws System.exit(). But for good static analysis may
     * be Neal is rigth about the Nothing type?
     */
    public abstract void usage();

    public abstract Option getOption(String value);

    public void analyzeParameters(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith(OptionInfo.OPTION_PREFIX)) {
                System.out.println("Error parsing parameter " + arg);
                usage();
            }
            final String optionName = arg.substring(OptionInfo.OPTION_PREFIX.length());
            Option option = null;
            try {
                option = getOption(optionName);
            } catch (IllegalArgumentException e) {
                System.out.println("Unknown argument: " + arg);
                usage();
            }

            if (option.isNeedExtraParam()) {
                i++;
                if (i >= args.length) {
                    System.out.println("The option " + arg + " need a parameter " +
                            option.getParamDescription());
                    usage();
                }
                option.setValue(args[i]);
                if (option.getValue().startsWith(OptionInfo.OPTION_PREFIX)) {
                    System.out.println("The option " + arg + " need a parameter " +
                            option.getParamDescription());
                    usage();
                }
            } else {
                option.set();
            }
        }
    }

    protected void addOptionDescription(StringBuilder builder, Option[] optionList) {
        for (Option option : optionList) {
            builder.append(Option.OPTION_PREFIX).append(option.getName());
            if (option.isNeedExtraParam()) {
                builder.append(" [").append(option.getParamDescription()).append("]");
            }
            builder.append(" : ").append(option.getDescription()).append("\n");
        }
    }
}
