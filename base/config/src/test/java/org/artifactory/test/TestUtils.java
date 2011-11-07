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

package org.artifactory.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.artifactory.log.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Helper methods for testing.
 *
 * @author Yossi Shaul
 */
public class TestUtils {

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

    public static Object invokeMethodNoArgs(Object target, String methodName) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            Object result = method.invoke(target);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the {@link java.lang.reflect.Field field} with the given <code>name</code> on the
     * provided {@link Object target object} to the supplied <code>value</code>.
     * <p/>
     * Assumes the field is declared in the specified target class.
     *
     * @param target the target object on which to set the field
     * @param name   the name of the field to set
     * @param value  the value to set
     */
    public static void setField(Object target, String name, Object value) {
        try {
            Field field = findField(target.getClass(), name);
            if (field == null) {
                throw new IllegalArgumentException("Could not find field [" + name + "] on target [" + target + "]");
            }
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Attempt to find a {@link Field field} on the supplied {@link Class} with
     * the supplied <code>name</code>. Searches all
     * superclasses up to {@link Object}.
     *
     * @param clazz the class to introspect
     * @param name  the name of the field (may be <code>null</code> if type is specified)
     * @return the corresponding Field object, or <code>null</code> if not found
     */
    public static Field findField(Class<?> clazz, String name) {
        Class<?> searchType = clazz;
        while (searchType != null) {
            Field[] fields = searchType.getDeclaredFields();
            for (Field field : fields) {
                if ((name == null || name.equals(field.getName()))) {
                    return field;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    public static void setLoggingLevel(Class clazz, Level level) {
        setLoggingLevel(clazz.getName(), level);
    }

    public static void setLoggingLevel(String name, Level level) {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger(name).setLevel(level);
    }

}
