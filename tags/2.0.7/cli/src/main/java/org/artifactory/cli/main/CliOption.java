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

import org.artifactory.cli.command.DumpCommand;
import org.artifactory.cli.common.Option;
import org.artifactory.cli.common.OptionInfo;

/**
 * @author freds
 * @date Sep 1, 2008
 */
public enum CliOption implements Option {
    host(
            "The remote artifactory server IP or host name with optional port number. Default: localhost:8081",
            "host name/ip address"),
    ssl(
            "Use https instead of http. Default is false"),
    timeout(
            "Set the timeout of the HTTP connection. Default: 900 seconds = 15 minutes.",
            "network timeout secs"),
    url(
            "The root URL of the artifactory REST API. The default is http://[host]/artifactory/api",
            "rest api root url"),
    username(
            "Username to use when connecting to a remote artifactory",
            "username"),
    password(
            "Password to use (clear) when connecting to a remote artifactory",
            "password"),
    noMetadata(
            "Exclude artifactory specific metadata when importing/exporting. Default: false"),
    symlinks(
            "Use symbolic links to the original import path file (no file copying done). Default: false"),
    syncImport(
            "Import directly into artifactory without using the background import process. Default: false"),
    bypassFiltering(
            "Ignore exiting repository content filtering rules during the export process. Default: false"),
    createArchive(
            "Zip the resulting folder after the export (very slow)"),
    dest(
            "The destination folder for the new export files. Default: dumpExport",
            "destination folder"),
    version(
            "The version of the old artifactory if it cannot be automatically determined", "version name"),
    repos(
            "Export only a specified list of repositories. Default: " + DumpCommand.EXPORT_ALL_NON_CACHED_REPOS,
            "repo names separated by ':'"),
    noconvert(
            "Do not auto convert local repository names of artifactory v1.2.5 and earlier to Virtual repositories. " +
                    "Default: false"),
    security(
            "Only dump a security definitions file, and no repositories. Default: false"),
    caches(
            "Include cached repositories in the export (by default caches are not exported). " +
                    "If the repos option is used this option will be ignored. Default: false"),
    verbose(
            "Display maximum execution details. Default: false"),
    failOnError(
            "Fail on the first error. Default: false"),
    failIfEmpty(
            "Fail when encountering empty source directories. Default: false"),
    update(
            "Post a file with update data to the artifactory server",
            "path to update data file"),
    destFile(
            "The destination file for this command's result.",
            "dest file path"),
    overwrite(
            "When set, if the destination file exists, the command will overwrite it."),
    m2(
            "Create .m2 compatible metadata (sha1, md5 and maven-metadata.xml files) when exporting");

    private final Option option;

    CliOption(String description) {
        this.option = new OptionInfo(name(), description);
    }

    CliOption(String description, String paramDescription) {
        this.option = new OptionInfo(name(), description, true, paramDescription);
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
