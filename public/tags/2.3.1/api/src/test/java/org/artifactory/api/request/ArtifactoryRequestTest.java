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

package org.artifactory.api.request;

import org.artifactory.md.Properties;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

/**
 * @author Yoav Landman
 */
@Test
public class ArtifactoryRequestTest extends ArtifactoryHomeBoundTest {

    public void matrixParams() {
        ArtifactoryRequestBase request = newRequest();
        request.setRepoPath(request.calculateRepoPath("libs-releases/path1/path2:metadata1;a=1;a=11;b=2;c;d=4;e"));
        Properties params = request.getProperties();
        assertMatrixParams(params);
        request = newRequest();
        request.setRepoPath(request.calculateRepoPath("libs-releases;a=1;a=11;b=2;c;d=4;e/path1/path2:metadata1"));
        params = request.getProperties();
        assertMatrixParams(params);
        request = newRequest();
        request.setRepoPath(request.calculateRepoPath("libs-releases;a=1;b=2/path1/path2:metadata1;a=11;c;d=4;e"));
        params = request.getProperties();
        assertMatrixParams(params);
        request = newRequest();
        request.setRepoPath(request.calculateRepoPath("libs-releases;a=1;a=11;b=2;c;d=4;e"));
        params = request.getProperties();
        assertMatrixParams(params);
    }

    public void matrixParamsWithEncodedValues() {
        ArtifactoryRequestBase request = newRequest();
        request.setRepoPath(request.calculateRepoPath("libs-releases;a=1+2;b=1%232;c="));
        Properties params = request.getProperties();
        assertEquals(params.size(), 3, "Expecting 3 parameters");
        assertEquals(params.get("a").iterator().next(), "1 2", "Character '+' should have been decoded to space");
        assertEquals(params.get("b").iterator().next(), "1#2", "String '%23' should have been decoded to '#'");
        assertEquals(params.get("c").iterator().next(), "", "Expecting empty string");
    }

    private void assertMatrixParams(Properties params) {
        assertEquals(params.size(), 6);
        assertEquals(params.get("a").toArray(), new String[]{"1", "11"});
        assertEquals(params.get("b").iterator().next(), "2");
        assertEquals(params.get("c").iterator().next(), "");
        assertEquals(params.get("d").iterator().next(), "4");
        assertEquals(params.get("e").iterator().next(), "");
        assertFalse(params.get("f").iterator().hasNext());
    }

    private ArtifactoryRequestBase newRequest() {
        return new ArtifactoryRequestBase() {
            public long getLastModified() {
                return 0;
            }

            public long getIfModifiedSince() {
                return 0;
            }

            public String getSourceDescription() {
                return null;
            }

            public InputStream getInputStream() throws IOException {
                return null;
            }

            public boolean isHeadOnly() {
                return false;
            }

            public boolean isRecursive() {
                return false;
            }

            public boolean isFromAnotherArtifactory() {
                return false;
            }

            public int getContentLength() {
                return 0;
            }

            public String getHeader(String headerName) {
                return null;
            }

            public String getUri() {
                return null;
            }

            public String getServletContextUrl() {
                return "";
            }

            public String getParameter(String name) {
                return null;
            }
        };
    }

}
