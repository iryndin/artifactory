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

package org.artifactory.support.utils;

import org.testng.annotations.Test;
import static org.artifactory.util.StringUtils.LINE_SEPARATOR;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Michael Pasternak
 */
public class StringBuilderWrapperTest {

    @Test
    public void testAppendOnStringBuilderWrapper() {
        StringBuilderWrapper sb = new StringBuilderWrapper();
        sb.append("foo1", "bar1");
        sb.append("foo2", "bar2");

        assertEquals(sb.toString(), "foo1: bar1" +
                LINE_SEPARATOR + "foo2: bar2" + LINE_SEPARATOR
        );
    }
    @Test
    public void testCreateAndAppendOnStringBuilderWrapper() {
        StringBuilderWrapper sb = new StringBuilderWrapper("test1:test2" + LINE_SEPARATOR);
        sb.append("foo1", "bar1");
        sb.append("foo2", "bar2");

        assertEquals(sb.toString(),
                "test1:test2" + LINE_SEPARATOR + "foo1: bar1" +
                        LINE_SEPARATOR + "foo2: bar2" + LINE_SEPARATOR
        );
    }
}
