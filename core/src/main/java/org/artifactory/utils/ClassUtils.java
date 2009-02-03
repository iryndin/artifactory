/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.utils;

import org.apache.log4j.Logger;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class ClassUtils {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ClassUtils.class);

    private ClassUtils() {
        // Utility class
    }

    public static Field getAccessibleField(Class<?> clazz, String fieldName) {
        assert clazz != null;
        assert fieldName != null && fieldName.trim().length() > 0;
        final Field field;
        try {
            field = clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(
                    "Could not get field [" + fieldName + "] in class [" + clazz.getName() + "]: ",
                    e);
        }
        field.setAccessible(true);
        return field;
    }

    public static void setAccessibleField(
            String fieldName, Class fieldType, Object target, Object value) {
        Field field = ReflectionUtils.findField(target.getClass(), fieldName, fieldType);
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, target, value);
    }

}