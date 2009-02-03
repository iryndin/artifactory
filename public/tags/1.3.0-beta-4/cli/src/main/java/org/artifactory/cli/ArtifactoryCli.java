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

import com.thoughtworks.xstream.XStream;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.utils.PathUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * User: freds Date: Aug 12, 2008 Time: 5:36:40 PM
 */
public class ArtifactoryCli {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger LOGGER =
            LogManager.getLogger(ArtifactoryCli.class);

    private static final OptionParser optionParser = new CliOptionParser();

    private static final String SERVER_HOST =
            "localhost:8081/";
    private static final String API_URI =
            "artifactory/api/";

    private static class CliOptionParser extends OptionParser {
        @Override
        public void usage() {
            final StringBuilder builder =
                    new StringBuilder("Usage: \njava ").append(ArtifactoryCli.class.getName())
                            .append("\n\n");
            Option[] optionList = CliOption.values();
            addOptionDescription(builder, optionList);
            builder.append("\nYou need to specify one of the following command parameters: ")
                    .append(CliOption.info.argValue()).append(", ")
                    .append(CliOption.imp.argValue()).append(" or ")
                    .append(CliOption.export.argValue()).append("\n");
            builder.append("\n");
            System.out.println(builder.toString());
            System.exit(-1);
        }

        @Override
        public Option getOption(String value) {
            if (value.equals(CliOption.imp.getName())) {
                return CliOption.imp;
            }
            return CliOption.valueOf(value);
        }
    }

    public static void main(String[] args) {
        optionParser.analyzeParameters(args);
        // TODO: A command option system
        optionParser.checkExclusive(CliOption.info, CliOption.imp);
        optionParser.checkExclusive(CliOption.info, CliOption.export);
        optionParser.checkExclusive(CliOption.export, CliOption.imp);
        optionParser.checkExclusive(CliOption.syncImport, CliOption.symlinks);
        StringBuilder url = new StringBuilder();
        if (CliOption.url.isSet()) {
            String passedUrl = CliOption.url.getValue().trim();
            addUrl(url, passedUrl);
        } else {
            if (CliOption.ssl.isSet()) {
                url.append("https://");
            } else {
                url.append("http://");
            }
            if (CliOption.server.isSet()) {
                String passedUrl = CliOption.server.getValue().trim();
                addUrl(url, passedUrl);
            } else {
                url.append(SERVER_HOST);
            }
            url.append(API_URI);
        }
        try {
            if (CliOption.info.isSet()) {
                info(url.toString());
            } else if (CliOption.imp.isSet()) {
                importFrom(url.toString());
            } else if (CliOption.export.isSet()) {
                exportTo(url.toString());
            } else {
                System.err.println("No command was specified. Please specify a command");
                optionParser.usage();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void addUrl(StringBuilder url, String passedUrl) {
        url.append(passedUrl);
        if (passedUrl.charAt(passedUrl.length() - 1) != '/') {
            url.append("/");
        }
    }

    public static void info(String apiRoot) throws Exception {
        String systemUri = apiRoot + "system";
        get(systemUri, null);
    }

    public static void exportTo(String apiRoot) throws Exception {
        String systemUri = apiRoot + "system/export";
        File exportTo = new File(CliOption.export.getValue());
        if (exportTo.exists()) {
            exportTo = new File(exportTo.getCanonicalPath());
        }
        ExportSettings settings = new ExportSettings(exportTo);
        if (CliOption.noMetadata.isSet()) {
            settings.setIncludeMetadata(false);
        }
        if (CliOption.createArchive.isSet()) {
            settings.setCreateArchive(true);
        }
        if (CliOption.bypassFiltering.isSet()) {
            settings.setIgnoreRepositoryFilteringRulesOn(true);
        }
        // TODO: The repo list
        //settings.setReposToExport();
        post(systemUri, settings, null);
    }

    public static void importFrom(String apiRoot) throws Exception {
        String systemUri = apiRoot + "system/import";
        File importFrom = new File(CliOption.imp.getValue());
        if (importFrom.exists()) {
            importFrom = new File(importFrom.getCanonicalPath());
        }
        ImportSettings settings = new ImportSettings(importFrom);
        if (CliOption.noMetadata.isSet()) {
            settings.setIncludeMetadata(false);
        }
        if (CliOption.symlinks.isSet()) {
            settings.setUseSymLinks(true);
            settings.setCopyToWorkingFolder(true);
        }
        if (CliOption.syncImport.isSet()) {
            settings.setUseSymLinks(false);
            settings.setCopyToWorkingFolder(false);
        }
        // TODO: The repo list
        //settings.setReposToImport();
        post(systemUri, settings, null);
    }

    @SuppressWarnings({"unchecked"})
    protected static <I, O> O post(String uri, I inObj, Class<O> outObjClass)
            throws Exception {
        XStream xStream = new XStream();
        xStream.processAnnotations(inObj.getClass());
        if (outObjClass != null) {
            xStream.processAnnotations(outObjClass);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        xStream.toXML(inObj, bos);
        if (outObjClass != null) {
            byte[] bytes = post(uri, bos.toByteArray(), "application/xml", 200, "application/xml",
                    false);
            return (O) xStream.fromXML(new ByteArrayInputStream(bytes));
        } else {
            post(uri, bos.toByteArray(), "application/xml", 200, null, true);
            return null;
        }
    }

    @SuppressWarnings({"unchecked"})
    protected static <T> T get(String uri, Class<T> xstreamObjClass)
            throws Exception {
        if (xstreamObjClass != null) {
            byte[] bytes = get(uri, 200, "application/xml", false);
            XStream xStream = new XStream();
            xStream.processAnnotations(xstreamObjClass);
            return (T) xStream.fromXML(new ByteArrayInputStream(bytes));
        } else {
            get(uri, 200, null, true);
            return null;
        }
    }

    protected static byte[] get(String uri, int expectedStatus,
            String expectedMediaType, boolean printStream) throws Exception {
        GetMethod method = new GetMethod(uri);

        executeMethod(uri, method, expectedStatus, expectedMediaType);

        return analyzeResponse(method, printStream);
    }

    private static void checkContentType(String uri, String expectedMediaType, String contentType) {
        if (!PathUtils.hasText(expectedMediaType)) {
            return;
        }
        if (!contentType.contains(expectedMediaType)) {
            throw new RuntimeException("HTTP content type was " + contentType + " and should be " +
                    expectedMediaType + " for request on " + uri);
        }
    }

    private static void checkStatus(String uri, int expectedStatus, int status) {
        if (status != expectedStatus) {
            throw new RuntimeException("HTTP status code was " + status + " and should be " +
                    expectedStatus + " for request on " + uri);
        }
    }

    protected static byte[] post(String uri, final byte[] data,
            final String inputDataType,
            int expectedStatus,
            String expectedMediaType, boolean printStream) throws Exception {
        PostMethod method = new PostMethod(uri);
        method.setRequestEntity(new RequestEntity() {
            public boolean isRepeatable() {
                return true;
            }

            public void writeRequest(OutputStream out) throws IOException {
                out.write(data);
            }

            public long getContentLength() {
                return data.length;
            }

            public String getContentType() {
                return inputDataType;
            }
        });
        executeMethod(uri, method, expectedStatus, expectedMediaType);

        return analyzeResponse(method, printStream);
    }

    private static byte[] analyzeResponse(HttpMethod method, boolean printStream)
            throws IOException {
        InputStream is = method.getResponseBodyAsStream();
        byte[] buffer = new byte[1024];
        int r;
        if (printStream) {
            while ((r = is.read(buffer)) != -1) {
                System.out.println(new String(buffer, 0, r, "utf-8"));
            }
            return null;
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((r = is.read(buffer)) != -1) {
                baos.write(buffer, 0, r);
            }

            return baos.toByteArray();
        }
    }

    private static void executeMethod(String uri, HttpMethod method, int expectedStatus,
            String expectedMediaType) throws Exception {
        int status = getHttpClient(uri).executeMethod(method);
        checkStatus(uri, expectedStatus, status);
        Header mediaType = method.getResponseHeader("content-type");
        String contentType = mediaType.getValue();
        checkContentType(uri, expectedMediaType, contentType);
    }

    protected static HttpClient getHttpClient(String url) throws Exception {
        HttpConnectionManager connectionManager = new SimpleHttpConnectionManager();
        HttpConnectionManagerParams connectionManagerParams = connectionManager.getParams();
        //Set the socket connection timeout
        connectionManagerParams.setConnectionTimeout(3000);
        HttpClient client = new HttpClient(connectionManager);
        HttpClientParams clientParams = client.getParams();
        //Set the socket data timeout
        int to = 30000;
        if (CliOption.timeout.isSet()) {
            to = Integer.parseInt(CliOption.timeout.getValue()) * 1000;
        }
        clientParams.setSoTimeout(to);
        if (CliOption.username.isSet()) {
            String host = new URL(url).getHost();
            clientParams.setAuthenticationPreemptive(true);
            Credentials creds = new UsernamePasswordCredentials(
                    CliOption.username.getValue(),
                    CliOption.password.getValue());
            AuthScope scope = new AuthScope(host, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
            client.getState().setCredentials(scope, creds);
        }
        return client;
    }
}
