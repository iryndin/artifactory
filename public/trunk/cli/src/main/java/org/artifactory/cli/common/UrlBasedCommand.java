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

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.lang.ArrayUtils;
import org.artifactory.api.rest.constant.RestConstants;
import org.artifactory.cli.main.CliOption;
import org.artifactory.cli.main.CommandDefinition;
import org.artifactory.cli.rest.RestClient;
import org.artifactory.util.RemoteCommandException;

/**
 * Extends the BaseCommand class to act as a super class for commands that need URL handling (import, export, etc')
 *
 * @author Noam Tenne
 */
public abstract class UrlBasedCommand extends BaseCommand {

    /**
     * Default server host
     */
    private static final String SERVER_HOST = "localhost:8081/";

    /**
     * URL String Builder
     */
    private StringBuilder url = new StringBuilder();

    /**
     * Default constructor
     *
     * @param commandDefinition The command defintion
     * @param extraOptions      Extra CLI option (if needed, as well as the global ones)
     */
    public UrlBasedCommand(CommandDefinition commandDefinition, CliOption... extraOptions) {
        super(commandDefinition, addExtra(extraOptions));
    }

    /**
     * Recieves the extra options from the command class and adds it to the existing global ones
     *
     * @param extraOptions Any needed extra CLI options
     * @return CliOption[] Options needed for the command
     */
    protected static CliOption[] addExtra(CliOption... extraOptions) {
        CliOption[] baseOptions = {
                CliOption.username,
                CliOption.password,
                CliOption.host,
                CliOption.ssl,
                CliOption.timeout,
                CliOption.url
        };
        CliOption[] allOptions = (CliOption[]) ArrayUtils.addAll(baseOptions, extraOptions);
        return allOptions;
    }

    /**
     * Handles the URL
     *
     * @param url
     * @param passedUrl
     */
    private static void addUrl(StringBuilder url, String passedUrl) {
        url.append(passedUrl);
        if (passedUrl.charAt(passedUrl.length() - 1) != '/') {
            url.append("/");
        }
    }

    /**
     * Returns the URL
     *
     * @return String Host URL
     */
    public String getUrl() {
        if (CliOption.url.isSet()) {
            String passedUrl = CliOption.url.getValue().trim();
            addUrl(url, passedUrl);
        } else {
            if (CliOption.ssl.isSet()) {
                url.append("https://");
            } else {
                url.append("http://");
            }
            if (CliOption.host.isSet()) {
                String passedUrl = CliOption.host.getValue().trim();
                addUrl(url, passedUrl);
            } else {
                url.append(SERVER_HOST);
            }
            url.append("artifactory/" + RestConstants.PATH_API + "/");
        }
        return url.toString();
    }

    @SuppressWarnings({"unchecked"})
    protected static <I, O> O post(String uri, I inObj, Class<O> outObjClass) throws Exception {
        int timeOut = getTimeOut();
        Credentials credentials = getCredentials();
        O o = RestClient.post(uri, inObj, outObjClass, timeOut, credentials);
        return o;
    }

    @SuppressWarnings({"unchecked"})
    protected static <T> T get(String uri, Class<T> xstreamObjClass) throws Exception {
        int timeOut = getTimeOut();
        Credentials credentials = getCredentials();
        T t = RestClient.get(uri, xstreamObjClass, timeOut, credentials);
        return t;
    }

    protected static byte[] get(String uri, int expectedStatus, String expectedMediaType, boolean printStream)
            throws Exception {
        int timeOut = getTimeOut();
        Credentials credentials = getCredentials();
        byte[] analyzedResponse =
                RestClient.get(uri, expectedStatus, expectedMediaType, printStream, timeOut, credentials);
        return analyzedResponse;
    }

    protected static byte[] post(String uri, final byte[] data, final String inputDataType, int expectedStatus,
            String expectedMediaType, boolean printStream) throws Exception {
        int timeOut = getTimeOut();
        Credentials credentials = getCredentials();
        byte[] analyzedResponse = RestClient.post(uri, data, inputDataType, expectedStatus, expectedMediaType,
                printStream, timeOut, credentials);

        return analyzedResponse;
    }

    protected static int getTimeOut() {
        int to = -1;
        if (CliOption.timeout.isSet()) {
            String timeout = CliOption.timeout.getValue();
            try {
                to = Integer.parseInt(timeout) * 1000;
            } catch (NumberFormatException nfe) {
                throw new RemoteCommandException(
                        "\nThe timeout length you have entered: " + timeout + " - is invalid.\n" +
                                "Please enter a positive integer for a timeout value in seconds.");
            }
        }

        return to;
    }

    protected static Credentials getCredentials() {
        if (CliOption.username.isSet()) {
            Credentials creds = new UsernamePasswordCredentials(
                    CliOption.username.getValue(),
                    CliOption.password.getValue());

            return creds;
        }

        return null;
    }
}