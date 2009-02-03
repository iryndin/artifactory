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
public enum CliOption implements Option {
    info("Display general system information about Artifactory"),
    imp("import", "Activate full system import from a designated path", true, "import from path"),
    export("Activate full system export to path", true, "export to path"),
    server("The remote Artifactory server IP or host name with optional port number. " +
            "The default is localhost:8081", true, "the server host or ip"),
    ssl("Activate https instead of http. Default is false"),
    timeout("Set the timeout of the HTTP connection.", true, "timeout in seconds"),
    url("The root URL of the Artifactry REST API. " +
            "The default is http://[servername]/artifactory/api", true, "root url to rest api"),
    username("Optional username to use when connecting to the remote Artifactory", true,
            "username"),
    password("The users's clear text password", true, "password"),
    noMetadata("Exclude metadata information when importing/exporting"),
    symlinks("Use symbolic links to the original import path file (no file copying)"),
    syncImport("Import directly into Artifactory without using the background import process"),
    bypassFiltering("Avoid using exiting repository filtering rules during the export process"),
    createArchive("Zip the resulting folder after the export (slow)");

    private final Option option;

    CliOption(String description) {
        this.option = new OptionInfo(name(), description);
    }

    CliOption(String description, boolean needExtraParam, String paramDescription) {
        this.option = new OptionInfo(name(), description, needExtraParam, paramDescription);
    }

    CliOption(String optName, String description, boolean needExtraParam, String paramDescription) {
        this.option = new OptionInfo(optName, description, needExtraParam, paramDescription);
    }

    public String getDescription() {
        return option.getDescription();
    }

    public boolean isNeedExtraParam() {
        return option.isNeedExtraParam();
    }

    public String getParamDescription() {
        return option.getParamDescription();
    }

    public void setValue(String value) {
        option.setValue(value);
    }

    public String getValue() {
        return option.getValue();
    }

    public boolean isSet() {
        return option.isSet();
    }

    public String argValue() {
        return option.argValue();
    }

    public String getName() {
        return option.getName();
    }

    public void set() {
        option.set();
    }
}
