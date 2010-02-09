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

package org.artifactory.util;

import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author yoavl
 */
public class ResourceUtils {

    public static void copyResource(String resourcePath, File outputFile) throws IOException {
        copyResource(resourcePath, null, outputFile, null);
    }

    public static void copyResource(String resourcePath, File outputFile, InputStreamManipulator manipulator)
            throws IOException {
        copyResource(resourcePath, null, outputFile, manipulator);
    }

    public static void copyResource(String resourcePath, Class clazz, File outputFile) throws IOException {
        copyResource(resourcePath, clazz, outputFile, null);
    }

    public static void copyResource(
            String resourcePath, Class clazz, File outputFile, InputStreamManipulator manipulator) throws IOException {
        InputStream origInputStream = null;
        InputStream usedInputStream = null;
        OutputStream os = null;
        try {
            origInputStream = clazz != null ?
                    clazz.getResourceAsStream(resourcePath) :
                    Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
            if (origInputStream == null) {
                throw new IllegalArgumentException("Could not find the classpath resource at: " + resourcePath + ".");
            }
            if (manipulator != null) {
                InputStream mip = manipulator.manipulate(origInputStream);
                if (mip == null) {
                    throw new RuntimeException("Receicved a null stream from stream manipulation");
                }
                usedInputStream = mip;
            } else {
                usedInputStream = origInputStream;
            }
            os = new BufferedOutputStream(new FileOutputStream(outputFile));
            IOUtils.copy(usedInputStream, os);
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(usedInputStream);
            IOUtils.closeQuietly(origInputStream);
        }
    }

    public interface InputStreamManipulator {
        InputStream manipulate(InputStream origStream) throws IOException;
    }

}