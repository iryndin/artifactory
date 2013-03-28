/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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
import com.google.common.io.Files;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.io.FileUtils;
import org.artifactory.util.ResourceUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Test {@link HtmlRepositoryBrowser}.
 *
 * @author Tomer Cohen
 */
@Test
public class HtmlRepositoryBrowserTest {

    private HtmlRepositoryBrowser urlLister;
    private String baseUrl;

    @BeforeClass
    public void setUp() {
        final HttpClient hc = new HttpClient();
        HttpExecutor httpExecutor = new HttpExecutor() {
            @Override
            public int executeMethod(HttpMethod method) throws IOException {
                return hc.executeMethod(method);
            }
        };
        urlLister = new HtmlRepositoryBrowser(httpExecutor);
        baseUrl = "http://blabla";
    }

    public void listAllInvalid() throws IOException {
        File html = ResourceUtils.getResourceAsFile(testResourcesPath + "/invalidHtml.html");
        List<RemoteItem> urls = urlLister.parseHtml(Files.toString(html, Charsets.UTF_8), baseUrl);
        assertEquals(urls.size(), 0, "there should be no URLs in this html");
    }

    public void listAllNoHrefs() throws IOException {
        File html = ResourceUtils.getResourceAsFile(testResourcesPath + "/noHrefs.html");
        List<RemoteItem> urls = urlLister.parseHtml(Files.toString(html, Charsets.UTF_8), baseUrl);
        assertEquals(urls.size(), 0, "there should be no URLs in this html");
    }

    public void listAll() throws IOException {
        File html = ResourceUtils.getResourceAsFile(testResourcesPath + "/simple.html");
        File validHtml = createValidHtml(html);
        List<RemoteItem> urls = urlLister.parseHtml(Files.toString(validHtml, Charsets.UTF_8), baseUrl);
        assertEquals(urls.size(), 1);
    }

    public void listAllFromArtifactorySimpleBrowsingHtml() throws IOException {
        File html = ResourceUtils.getResourceAsFile(testResourcesPath + "/artifactory-simple.html");
        // base url that matches the links un the test html file
        String baseUrl = "http://localhost:8081/artifactory/libs-releases-local/org/jfrog/test/";
        List<RemoteItem> children = urlLister.parseHtml(Files.toString(html, Charsets.UTF_8), baseUrl);
        assertEquals(children.size(), 4, "Found: " + children);
        assertEquals(children.get(0), new RemoteItem(baseUrl + "multi1/", true));
        assertEquals(children.get(3), new RemoteItem(baseUrl + "multi.pom", false));
    }

    public void listAllFromArtifactoryListBrowsingHtml() throws IOException {
        File html = ResourceUtils.getResourceAsFile(testResourcesPath + "/artifactory-list.html");
        List<RemoteItem> children = urlLister.parseHtml(Files.toString(html, Charsets.UTF_8), baseUrl);
        assertEquals(children.size(), 3, "Found: " + children);
        assertEquals(children.get(0).getUrl(), baseUrl + "/multi1/");
        assertEquals(children.get(1).getUrl(), baseUrl + "/multi2/");
        assertEquals(children.get(2).getUrl(), baseUrl + "/multi3/");
    }

    private File createValidHtml(File source) throws IOException {
        File tempFile = File.createTempFile("artifactory", "html");
        String fileContent = FileUtils.readFileToString(source);
        fileContent = fileContent.replace("{placeHolder}", tempFile.toURI().toURL().toExternalForm());
        FileUtils.writeStringToFile(tempFile, fileContent);
        return tempFile;
    }

    private static final String testResourcesPath = "/org/artifactory/repo/remote/browse/html";


}
