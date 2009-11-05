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

package org.artifactory.api.mime;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author freds
 * @date Sep 28, 2008
 */
public class MimeTableTest {

    @Test
    public void xmlAppTest() {
        ContentType ct = NamingUtils.getContentType(new File("/tmp/anXmlFile.xml"));
        assertNotNull(ct);
        assertTrue(ct.isXml());
        assertEquals(ct.getMimeType(), "application/xml");
    }
}
