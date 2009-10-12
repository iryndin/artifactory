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

package org.artifactory.test;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * Helper methods for testing.
 *
 * @author Yossi Shaul
 */
public class TestUtils {

    public static InputStream getResource(String path) {
        InputStream is = TestUtils.class.getResourceAsStream(path);
        if (is == null) {
            throw new RuntimeException("Cannot find resource " + path);
        }
        return is;
    }

    public static File getResourceAsFile(String path) {
        URL resource = TestUtils.class.getResource(path);
        if (resource == null) {
            throw new RuntimeException("Resource not found: " + path);
        }
        return new File(resource.getFile());
    }

    public static Object invokeStaticMethod(Class<?> clazz, String methodName, Class[] paramTypes, Object[] params) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            Object result = method.invoke(null, params);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object invokeStaticMethodNoArgs(Class<?> clazz, String methodName) {
        return invokeStaticMethod(clazz, methodName, null, null);
    }

}
