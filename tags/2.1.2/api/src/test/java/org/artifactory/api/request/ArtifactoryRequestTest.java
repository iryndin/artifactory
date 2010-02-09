/*
 * This file is part of Artifactory.
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

import com.google.common.collect.SetMultimap;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Yoav Landman
 */

public class ArtifactoryRequestTest {

    @Test
    public void testMatrixParams() {
        ArtifactorySystemProperties.bind(new ArtifactorySystemProperties());
        ArtifactoryRequestBase request = newRequest();
        request.setRepoPath(request.calculateRepoPath("libs-releases/path1/path2:metadata1;a=1;a=11;b=2;c;d=4;e"));
        SetMultimap<String, String> params = request.getMatrixParams();
        assertMatrixParams(params);
        request = newRequest();
        request.setRepoPath(request.calculateRepoPath("libs-releases;a=1;a=11;b=2;c;d=4;e/path1/path2:metadata1"));
        params = request.getMatrixParams();
        assertMatrixParams(params);
        request = newRequest();
        request.setRepoPath(request.calculateRepoPath("libs-releases;a=1;b=2/path1/path2:metadata1;a=11;c;d=4;e"));
        params = request.getMatrixParams();
        assertMatrixParams(params);
        request = newRequest();
        request.setRepoPath(request.calculateRepoPath("libs-releases;a=1;a=11;b=2;c;d=4;e"));
        params = request.getMatrixParams();
        assertMatrixParams(params);
        ArtifactorySystemProperties.unbind();
    }

    private void assertMatrixParams(SetMultimap<String, String> params) {
        Assert.assertEquals(params.size(), 6);
        Assert.assertEquals(params.get("a").toArray(), new String[]{"1", "11"});
        Assert.assertEquals(params.get("b").iterator().next(), "2");
        Assert.assertEquals(params.get("c").iterator().next(), "");
        Assert.assertEquals(params.get("d").iterator().next(), "4");
        Assert.assertEquals(params.get("e").iterator().next(), "");
        Assert.assertFalse(params.get("f").iterator().hasNext());
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

            public boolean isWebdav() {
                return false;
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
