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

package org.artifactory.cli.main;

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
            "Use https instead of http. Default is false (do not use with the url option - specify the url directly)."),
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
    bypassFiltering(
            "Ignore exiting repository content filtering rules during the export process. Default: false"),
    createArchive(
            "Zip the resulting folder after the export (very slow)"),
    version(
            "The version of the old artifactory if it cannot be automatically determined", "version name"),
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
            "Create .m2 compatible metadata (sha1, md5 and maven-metadata.xml files) when exporting"),
    incremental("When exporting, this flag will determine if the export should be incremental or not");

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
