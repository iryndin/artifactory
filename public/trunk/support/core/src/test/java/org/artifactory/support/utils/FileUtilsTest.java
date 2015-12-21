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

import com.beust.jcommander.internal.Lists;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Michael Pasternak
 */
public class FileUtilsTest {

    List<File> files;

    @BeforeClass
    public void init() {
        files = Lists.newArrayList();
        files.add(new File(File.separator + "a"+
                File.separator+"b"+
                File.separator+"c"+
                File.separator+"foo.zip"));
        files.add(new File(File.separator + "a"+
                File.separator+"b"+
                File.separator+"c"+
                File.separator+"bar.zip"));
    }

    @Test
    public void toFileNamesTest() {
        List<String> fileNames = FileUtils.toFileNames(files);
        assertSize(fileNames);
        assertTrue(fileNames.contains("foo.zip"));
        assertTrue(fileNames.contains("bar.zip"));
    }

    private void assertSize(List<String> contents) {
        assertNotNull(contents);
        assertTrue(contents.size() == 2);
    }
}
