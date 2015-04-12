/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2015 JFrog Ltd.
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
package org.artifactory.util;

import org.apache.commons.codec.EncoderException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Tests the helper {@link HttpUtils} class.
 */
@Test
public class HttpUtilsTest {

    public void encodeUrlWithParameters() throws EncoderException {
        String unescaped = "http://domain:8089/context/path?x=a/b/c&b=c";
        String escaped = HttpUtils.encodeQuery(unescaped);
        assertEquals(escaped, unescaped);
    }

    public void encodeUrlWithSpaces() throws EncoderException {
        String unescaped = "http://domain:8089/context/file path.txt";
        String escaped = HttpUtils.encodeQuery(unescaped);
        assertEquals(escaped, "http://domain:8089/context/file%20path.txt");
    }

    public void encodeBuildUrl() throws EncoderException {
        String unescaped = "http://127.0.0.1:53123/artifactory/api/build/moo :: moo/999";
        String escaped = HttpUtils.encodeQuery(unescaped);
        assertEquals(escaped, "http://127.0.0.1:53123/artifactory/api/build/moo%20::%20moo/999");
    }
}