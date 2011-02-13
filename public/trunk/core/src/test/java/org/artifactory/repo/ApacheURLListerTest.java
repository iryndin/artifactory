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

package org.artifactory.repo;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.io.FileUtils;
import org.artifactory.util.ResourceUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Test {@link org.artifactory.repo.ApacheURLLister}.
 *
 * @author Tomer Cohen
 */
@Test
public class ApacheURLListerTest {

    private ApacheURLLister urlLister;
    private URL baseUrl;

    @BeforeClass
    public void setUp() throws MalformedURLException {
        urlLister = new ApacheURLLister(new HttpClient());
        baseUrl = new URL("http://blabla");
    }

    public void listAllInvalid() throws IOException {
        File html = ResourceUtils.getResourceAsFile("/urllister/invalidHtml.html");
        List<URL> urls = urlLister.parseHtml(Files.toString(html, Charsets.UTF_8), baseUrl, true, true);
        assertEquals(urls.size(), 0, "there should be no URLs in this html");
    }

    public void listAllNoHrefs() throws IOException {
        File html = ResourceUtils.getResourceAsFile("/urllister/noHrefs.html");
        List<URL> urls = urlLister.parseHtml(Files.toString(html, Charsets.UTF_8), baseUrl, true, true);
        assertEquals(urls.size(), 0, "there should be no URLs in this html");
    }

    public void listAll() throws IOException {
        File html = ResourceUtils.getResourceAsFile("/urllister/simple.html");
        File validHtml = createValidHtml(html);
        List<URL> urls = urlLister.parseHtml(Files.toString(validHtml, Charsets.UTF_8), baseUrl, true, true);
        assertEquals(urls.size(), 1);
    }

    public void listAllFromArtifactorySimpleBrowsingHtml() throws IOException {
        File html = ResourceUtils.getResourceAsFile("/urllister/artifactory-simple.html");
        List<URL> urls = urlLister.parseHtml(Files.toString(html, Charsets.UTF_8), baseUrl, true, true);
        //TODO: [by ys] this includes also the breadcrums urls. should test how tools handle this
        assertEquals(urls.size(), 7, "Found: " + urls);
    }

    public void listAllFromArtifactoryListBrowsingHtml() throws IOException {
        File html = ResourceUtils.getResourceAsFile("/urllister/artifactory-list.html");
        List<URL> urls = urlLister.parseHtml(Files.toString(html, Charsets.UTF_8), baseUrl, true, true);
        assertEquals(urls.size(), 3, "Found: " + urls);
        assertEquals(urls.get(0), new URL(baseUrl, "multi1/"));
        assertEquals(urls.get(1), new URL(baseUrl, "multi2/"));
        assertEquals(urls.get(2), new URL(baseUrl, "multi3/"));
    }

    private File createValidHtml(File source) throws IOException {
        File tempFile = File.createTempFile("artifactory", "html");
        String fileContent = FileUtils.readFileToString(source);
        fileContent = fileContent.replace("{placeHolder}", tempFile.toURI().toURL().toExternalForm());
        FileUtils.writeStringToFile(tempFile, fileContent);
        return tempFile;
    }

}
