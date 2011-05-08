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

package org.artifactory.repo.remote.browse;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.XmlUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Support browsing Amazon S3 repositories.<p/>
 * For more details see: <a href="http://docs.amazonwebservices.com/AmazonS3/latest/API/APIRest.html">Amazon S3 API</a>.
 * <p/>
 * Bucket list API: <a href="http://docs.amazonwebservices.com/AmazonS3/latest/API/RESTBucketGET.html">Bucket List API</a>
 *
 * @author Yossi Shaul
 */
public class S3RepositoryBrowser extends RemoteRepositoryBrowser {
    private static final Logger log = LoggerFactory.getLogger(S3RepositoryBrowser.class);

    private static final String ERROR_CODE_NOSUCHKEY = "NoSuchKey";
    private static final String HEADER_S3_REQUEST_ID = "x-amz-request-id";

    /**
     * The root URL of the S3 repository. This is the bucket url on which list requests should be done.
     */
    private String rootUrl;

    public S3RepositoryBrowser(HttpClient client) {
        super(client);
    }

    @Override
    public List<RemoteItem> listContent(String url) throws IOException {
        if (rootUrl == null) {
            detectRootUrl(url);
        }
        url = forceDirectoryUrl(url);
        String s3Url = buildS3RequestUrl(url);
        log.debug("Request url: {} S3 url: {}", url, s3Url);
        String result = getContent(s3Url);
        log.debug("S3 result: {}", result);
        return parseResponse(result);
    }

    private String buildS3RequestUrl(String url) {
        // the s3 request should always go to the root and add the rest of the path as the prefix parameter.
        String prefixPath = StringUtils.removeStart(url, rootUrl);
        StringBuilder sb = new StringBuilder(rootUrl).append("?prefix=").append(prefixPath);

        // we assume a file system structure with '/' as the delimiter
        sb.append("&").append("delimiter=/");
        return sb.toString();
    }

    /**
     * Detects the bucket url (i.e., root url). The given url is assumed to either point to the root or to "directory"
     * under the root. The most reliable way to get the root is to request non-existing resource and analyze the response.
     *
     * @param url URL to S3 repository
     * @return The root url of the repository (the bucket)
     */
    String detectRootUrl(String url) throws IOException {
        // force non-directory url. S3 returns 200 for directory paths
        url = url.endsWith("/") ? StringUtils.removeEnd(url, "/") : url;
        // generate a random string to force 404
        String randomString = RandomStringUtils.randomAlphanumeric(16);
        url = url + "/" + randomString;
        GetMethod method = new GetMethod(url);
        try {
            // most likely to get 404 if the repository exists
            int statusCode = client.executeMethod(method);
            assertSizeLimit(url, method);
            String responseString = IOUtils.toString(method.getResponseBodyAsStream(), Charsets.UTF_8.name());
            log.debug("Detect S3 root url got response code {} with content: {}", statusCode, responseString);
            Document doc = XmlUtils.parse(responseString);
            Element root = doc.getRootElement();
            String errorCode = root.getChildText("Code", root.getNamespace());
            if (ERROR_CODE_NOSUCHKEY.equals(errorCode)) {
                String relativePath = root.getChildText("Key", root.getNamespace());
                rootUrl = StringUtils.removeEnd(url, relativePath);
            }
        } finally {
            method.releaseConnection();
        }
        log.debug("Detected S3 root URL: {}", rootUrl);
        return rootUrl;
    }

    /**
     * @param url    The URL to check
     * @param client Http client to use
     * @return True if the url points to an S3 repository.
     */
    public static boolean isS3Repository(String url, HttpClient client) {
        HeadMethod headMethod = new HeadMethod(url);
        try {
            client.executeMethod(headMethod);
            Header s3RequestId = headMethod.getResponseHeader(HEADER_S3_REQUEST_ID);
            return s3RequestId != null;
        } catch (IOException e) {
            log.debug("Failed detecting S3 repository: " + e.getMessage(), e);
        } finally {
            headMethod.releaseConnection();
        }
        return false;
    }

    @SuppressWarnings({"unchecked"})
    private List<RemoteItem> parseResponse(String content) {
        List<String> children = Lists.newArrayList();
        List<RemoteItem> items = Lists.newArrayList();
        Document document = XmlUtils.parse(content);
        Element root = document.getRootElement();
        Namespace ns = root.getNamespace();
        String prefix = root.getChildText("Prefix", ns);

        // retrieve folders
        List<Element> folders = root.getChildren("CommonPrefixes", ns);
        for (Element folder : folders) {
            String directoryPath = folder.getChildText("Prefix", ns);
            String folderName = StringUtils.removeStart(directoryPath, prefix);
            if (StringUtils.isNotBlank(folderName)) {
                children.add(rootUrl + directoryPath);
                items.add(new RemoteItem(rootUrl + directoryPath, true));
            }
        }

        // retrieve files
        List<Element> files = root.getChildren("Contents", ns);
        for (Element element : files) {
            String filePath = element.getChildText("Key", ns);
            String fileName = StringUtils.removeStart(filePath, prefix);
            if (StringUtils.isNotBlank(fileName) && !folderDirectoryWithSameNameExists(fileName, items)) {
                // the date format is of the form yyyy-mm-ddThh:mm:ss.timezone, e.g., 2009-02-03T16:45:09.000Z
                String sizeStr = element.getChildText("Size", ns);
                long size = sizeStr == null ? 0 : Long.parseLong(sizeStr);
                String lastModifiedStr = element.getChildText("LastModified", ns);
                long lastModified =
                        lastModifiedStr == null ? 0 : ISODateTimeFormat.dateTime().parseMillis(lastModifiedStr);
                children.add(rootUrl + filePath);
                items.add(new RemoteItem(rootUrl + filePath, false, size, lastModified));
            }
        }

        return items;
    }

    /**
     * some s3 repositories (e.g., terracotta http://repo.terracotta.org/?delimiter=/&prefix=maven2/) has files and
     * folders with the same name (for instance file named 'org' and directory named 'org/' under the same directory)
     * in such a case we prefer the directory and don't return the file
     */
    private boolean folderDirectoryWithSameNameExists(String fileName, List<RemoteItem> items) {
        for (RemoteItem item : items) {
            if (item.getName().equals(fileName)) {
                log.debug("Found file with the same name of a directory: {}", item.getUrl());
                return true;
            }
        }
        return false;
    }
}
